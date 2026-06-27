package br.com.byop.aegis.asset.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.asset.contract.UpdateAssetMetadataRequest;
import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.domain.AssetTag;
import br.com.byop.aegis.asset.domain.AssetTagAssignment;
import br.com.byop.aegis.asset.domain.AssetUsage;
import br.com.byop.aegis.asset.dto.AssetDetail;
import br.com.byop.aegis.asset.dto.AssetFileContent;
import br.com.byop.aegis.asset.dto.AssetSummary;
import br.com.byop.aegis.asset.dto.AssetUsageSummary;
import br.com.byop.aegis.asset.dto.ResolvedAsset;
import br.com.byop.aegis.asset.exception.AssetInUseException;
import br.com.byop.aegis.asset.exception.AssetNotFoundException;
import br.com.byop.aegis.asset.exception.AssetSizeLimitExceededException;
import br.com.byop.aegis.asset.exception.AssetTagAlreadyExistsException;
import br.com.byop.aegis.asset.exception.AssetTagNotFoundException;
import br.com.byop.aegis.asset.exception.InvalidAssetMimeTypeException;
import br.com.byop.aegis.asset.exception.InvalidAssetTagNameException;
import br.com.byop.aegis.asset.mapper.AssetMapper;
import br.com.byop.aegis.asset.repository.AssetRepository;
import br.com.byop.aegis.asset.repository.AssetTagAssignmentRepository;
import br.com.byop.aegis.asset.repository.AssetTagRepository;
import br.com.byop.aegis.asset.repository.AssetUsageRepository;
import br.com.byop.aegis.asset.storage.LocalStorageProvider;
import br.com.byop.aegis.asset.storage.ResolvedLocation;
import br.com.byop.aegis.asset.storage.StorageProvider;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASSET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2026-06-27T10:00:00Z");

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetTagRepository assetTagRepository;

    @Mock
    private AssetTagAssignmentRepository assignmentRepository;

    @Mock
    private AssetUsageRepository usageRepository;

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private AssetUploadValidator uploadValidator;

    @Mock
    private AssetStorageProvisioningService storageProvisioningService;

    @Mock
    private LocalStorageProvider localStorageProvider;

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private ProductAccessPort productAccessPort;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private AuditService auditService;

    private AssetService assetService;
    private AuthenticatedUser caller;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetRepository, assetTagRepository, assignmentRepository, usageRepository,
                assetMapper, uploadValidator, storageProvisioningService, localStorageProvider,
                productReferenceService, productAccessPort, auditService);
        caller = new AuthenticatedUser("subject-1", "user@aegis.app", "user", "User", Set.of("ROLE_EDITOR"));
    }

    @Test
    void shouldUploadValidAsset() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("application/pdf", 8L)).thenReturn(AssetCategory.PDF);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.store(eq(TENANT_ID), eq(PRODUCT_ID), eq(AssetCategory.PDF), eq("curriculo.pdf"), any()))
                .thenReturn("aegis/pms/x/y/pdf/curriculo.pdf");
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.toSummary(any(Asset.class), eq(List.of()), eq(List.of())))
                .thenReturn(new AssetSummary(null, "Currículo", "pdf", "1 KB", "ativo", "", "", "2026-06-27"));

        MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());
        AssetSummary result = assetService.uploadAsset(PRODUCT_ID, file, "Currículo", caller);

        assertThat(result.name()).isEqualTo("Currículo");
        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        Asset saved = assetCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("curriculo.pdf");
        assertThat(saved.getFriendlyName()).isEqualTo("Currículo");
        assertThat(saved.getStorageProvider()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(saved.getStorageKey()).isEqualTo("aegis/pms/x/y/pdf/curriculo.pdf");
        assertThat(saved.getUploadedBySubject()).isEqualTo("subject-1");
    }

    @Test
    void shouldUploadWithoutFriendlyNameKeepingOriginalAsDefault() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("image/png", 3L)).thenReturn(AssetCategory.IMAGE);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.store(any(), any(), any(), any(), any())).thenReturn("aegis/pms/x/y/image/foto.png");
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.toSummary(any(Asset.class), eq(List.of()), eq(List.of()))).thenReturn(
                new AssetSummary(null, "foto.png", "image", "1 KB", "ativo", "", "", "2026-06-27"));

        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", "abc".getBytes());
        assetService.uploadAsset(PRODUCT_ID, file, null, caller);

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getFriendlyName()).isEqualTo("foto.png");
    }

    @Test
    void shouldRejectUploadWithInvalidMimeType() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("application/x-msdownload", 3L)).thenThrow(new InvalidAssetMimeTypeException("application/x-msdownload"));

        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", "abc".getBytes());

        assertThatThrownBy(() -> assetService.uploadAsset(PRODUCT_ID, file, null, caller))
                .isInstanceOf(InvalidAssetMimeTypeException.class);
        verify(assetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUploadAboveSizeLimit() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("image/png", 999L)).thenThrow(new AssetSizeLimitExceededException(AssetCategory.IMAGE, 999L, 100L));

        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[999]);

        assertThatThrownBy(() -> assetService.uploadAsset(PRODUCT_ID, file, null, caller))
                .isInstanceOf(AssetSizeLimitExceededException.class);
        verify(assetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUploadWithNullOriginalFilename() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        // MockMultipartFile normaliza originalFilename nulo para "" no construtor — usar um mock
        // real para exercitar o ramo `originalFilename == null` (distinto do ramo "branco").
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> assetService.uploadAsset(PRODUCT_ID, file, null, caller))
                .isInstanceOf(br.com.byop.aegis.asset.exception.InvalidAssetFilenameException.class);
        verify(assetRepository, never()).save(any());
    }

    @Test
    void shouldRejectUploadWithBlankOriginalFilename() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        MockMultipartFile file = new MockMultipartFile("file", "   ", "image/png", "abc".getBytes());

        assertThatThrownBy(() -> assetService.uploadAsset(PRODUCT_ID, file, null, caller))
                .isInstanceOf(br.com.byop.aegis.asset.exception.InvalidAssetFilenameException.class);
        verify(assetRepository, never()).save(any());
    }

    @Test
    void shouldUploadWithBlankFriendlyNameKeepingOriginalAsDefault() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("image/png", 3L)).thenReturn(AssetCategory.IMAGE);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.store(any(), any(), any(), any(), any())).thenReturn("aegis/pms/x/y/image/foto.png");
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.toSummary(any(Asset.class), eq(List.of()), eq(List.of())))
                .thenReturn(new AssetSummary(null, "foto.png", "image", "1 KB", "ativo", "", "", "2026-06-27"));

        assetService.uploadAsset(PRODUCT_ID, new MockMultipartFile("file", "foto.png", "image/png", "abc".getBytes()), "   ", caller);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getFriendlyName()).isEqualTo("foto.png");
    }

    @Test
    void shouldUploadWithNullContentType() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("", 3L)).thenReturn(AssetCategory.DOCUMENT);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.store(any(), any(), any(), any(), any())).thenReturn("aegis/pms/x/y/document/arquivo");
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.toSummary(any(Asset.class), eq(List.of()), eq(List.of())))
                .thenReturn(new AssetSummary(null, "arquivo", "document", "1 KB", "ativo", "", "", "2026-06-27"));

        MockMultipartFile file = new MockMultipartFile("file", "arquivo", null, "abc".getBytes());
        assetService.uploadAsset(PRODUCT_ID, file, null, caller);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(captor.capture());
        assertThat(captor.getValue().getMimeType()).isEmpty();
    }

    @Test
    void shouldWrapIOExceptionWhenReadingUploadedFileFails() throws java.io.IOException {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        when(uploadValidator.validate("image/png", 3L)).thenReturn(AssetCategory.IMAGE);
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("foto.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(3L);
        when(file.getBytes()).thenThrow(new java.io.IOException("broken stream"));

        assertThatThrownBy(() -> assetService.uploadAsset(PRODUCT_ID, file, null, caller))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    @Test
    void shouldKeepStorageProviderRecordedAtUploadTimeAcrossStrategyChanges() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(uploadValidator.validate(any(), anyLong())).thenReturn(AssetCategory.IMAGE);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.S3)).thenReturn(storageProvider);
        when(storageProvider.store(any(), any(), any(), any(), any())).thenReturn("aegis/pms/x/y/image/foto.png");
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.toSummary(any(Asset.class), eq(List.of()), eq(List.of())))
                .thenReturn(new AssetSummary(null, "foto.png", "image", "1 KB", "ativo", "", "", "2026-06-27"));

        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.LOCAL);
        assetService.uploadAsset(PRODUCT_ID, new MockMultipartFile("file", "foto.png", "image/png", "1".getBytes()), null, caller);

        when(productReferenceService.getRequiredAssetStorageStrategy(PRODUCT_ID)).thenReturn(AssetStorageStrategy.S3);
        assetService.uploadAsset(PRODUCT_ID, new MockMultipartFile("file", "foto2.png", "image/png", "2".getBytes()), null, caller);

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Asset> savedAssets = captor.getAllValues();
        assertThat(savedAssets.get(0).getStorageProvider()).isEqualTo(AssetStorageStrategy.LOCAL);
        assertThat(savedAssets.get(1).getStorageProvider()).isEqualTo(AssetStorageStrategy.S3);
    }

    @Test
    void shouldListAssets() {
        Asset asset = asset();
        when(assetRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(asset));
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        AssetSummary summary = new AssetSummary(ASSET_ID, "foto.png", "image", "1 KB", "ativo", "", "", "2026-06-27");
        when(assetMapper.toSummary(asset, List.of(), List.of())).thenReturn(summary);

        List<AssetSummary> result = assetService.listAssets(PRODUCT_ID);

        assertThat(result).containsExactly(summary);
    }

    @Test
    void shouldGetAssetDetail() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        AssetDetail detail = detail();
        when(assetMapper.toDetail(asset, List.of(), List.of())).thenReturn(detail);

        assertThat(assetService.getAsset(PRODUCT_ID, ASSET_ID)).isEqualTo(detail);
    }

    @Test
    void shouldRejectGetAssetWhenNotFoundInProduct() {
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getAsset(PRODUCT_ID, ASSET_ID))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void shouldUpdateMetadataAndReconcileTags() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        UUID blogTagId = UUID.randomUUID();
        AssetTagAssignment existingAssignment = new AssetTagAssignment(ASSET_ID, blogTagId);
        when(assignmentRepository.findAllByAssetId(ASSET_ID))
                .thenReturn(List.of(existingAssignment))
                .thenReturn(List.of(existingAssignment));
        AssetTag blogTag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(blogTag, "id", blogTagId);
        when(assetTagRepository.findAllByIdIn(List.of(blogTagId))).thenReturn(List.of(blogTag));
        when(assetTagRepository.findByProductIdAndName(PRODUCT_ID, "institucional")).thenReturn(Optional.empty());
        AssetTag newTag = new AssetTag(PRODUCT_ID, "institucional");
        ReflectionTestUtils.setField(newTag, "id", UUID.randomUUID());
        when(assetTagRepository.save(any(AssetTag.class))).thenReturn(newTag);
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        AssetDetail detail = detail();
        when(assetMapper.toDetail(eq(asset), any(), any())).thenReturn(detail);

        UpdateAssetMetadataRequest request = new UpdateAssetMetadataRequest("Novo nome", "Alt", "Legenda", "Credito", "institucional");
        AssetDetail result = assetService.updateMetadata(PRODUCT_ID, ASSET_ID, request);

        assertThat(result).isEqualTo(detail);
        assertThat(asset.getFriendlyName()).isEqualTo("Novo nome");
        verify(assignmentRepository).deleteByAssetIdAndAssetTagId(ASSET_ID, blogTagId);
        verify(assignmentRepository).save(any(AssetTagAssignment.class));
    }

    @Test
    void shouldReuseExistingTagWhenReconcilingMetadataTags() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        UUID existingTagId = UUID.randomUUID();
        AssetTag existingTag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(existingTag, "id", existingTagId);
        when(assetTagRepository.findAllByIdIn(List.of())).thenReturn(List.of());
        when(assetTagRepository.findByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(Optional.of(existingTag));
        when(assignmentRepository.existsByAssetIdAndAssetTagId(ASSET_ID, existingTagId)).thenReturn(false);
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, "blog"));

        verify(assetTagRepository, never()).save(any(AssetTag.class));
        verify(assignmentRepository).save(any(AssetTagAssignment.class));
    }

    @Test
    void shouldNotDuplicateAssignmentWhenTagAlreadyAssigned() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        UUID tagId = UUID.randomUUID();
        AssetTagAssignment assignment = new AssetTagAssignment(ASSET_ID, tagId);
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of(assignment));
        AssetTag tag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(tag, "id", tagId);
        when(assetTagRepository.findAllByIdIn(List.of(tagId))).thenReturn(List.of(tag));
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, "blog"));

        verify(assignmentRepository, never()).save(any(AssetTagAssignment.class));
        verify(assignmentRepository, never()).deleteByAssetIdAndAssetTagId(any(), any());
    }

    @Test
    void shouldRemoveAssignmentWhenCurrentTagLookupIsMissing() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        UUID orphanTagId = UUID.randomUUID();
        AssetTagAssignment orphanAssignment = new AssetTagAssignment(ASSET_ID, orphanTagId);
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of(orphanAssignment));
        when(assetTagRepository.findAllByIdIn(List.of(orphanTagId))).thenReturn(List.of());
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, null));

        verify(assignmentRepository).deleteByAssetIdAndAssetTagId(ASSET_ID, orphanTagId);
    }

    @Test
    void shouldSkipAssignmentWhenTagAlreadyAssignedButMissingFromCurrentList() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetTagRepository.findAllByIdIn(List.of())).thenReturn(List.of());
        UUID tagId = UUID.randomUUID();
        AssetTag tag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(tag, "id", tagId);
        when(assetTagRepository.findByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(Optional.of(tag));
        when(assignmentRepository.existsByAssetIdAndAssetTagId(ASSET_ID, tagId)).thenReturn(true);
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, "blog"));

        verify(assignmentRepository, never()).save(any(AssetTagAssignment.class));
    }

    @Test
    void shouldClearAllTagsWhenMetadataTagsIsNull() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        UUID tagId = UUID.randomUUID();
        AssetTagAssignment assignment = new AssetTagAssignment(ASSET_ID, tagId);
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of(assignment));
        AssetTag tag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(tag, "id", tagId);
        when(assetTagRepository.findAllByIdIn(List.of(tagId))).thenReturn(List.of(tag));
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, null));

        verify(assignmentRepository).deleteByAssetIdAndAssetTagId(ASSET_ID, tagId);
    }

    @Test
    void shouldClearAllTagsWhenMetadataTagsIsBlankString() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        UUID tagId = UUID.randomUUID();
        AssetTagAssignment assignment = new AssetTagAssignment(ASSET_ID, tagId);
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of(assignment));
        AssetTag tag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(tag, "id", tagId);
        when(assetTagRepository.findAllByIdIn(List.of(tagId))).thenReturn(List.of(tag));
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, ""));

        verify(assignmentRepository).deleteByAssetIdAndAssetTagId(ASSET_ID, tagId);
    }

    @Test
    void shouldIgnoreEmptyEntriesInTagsCsv() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetTagRepository.findAllByIdIn(List.of())).thenReturn(List.of());
        when(assetTagRepository.findByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(Optional.empty());
        AssetTag newTag = new AssetTag(PRODUCT_ID, "blog");
        ReflectionTestUtils.setField(newTag, "id", UUID.randomUUID());
        when(assetTagRepository.save(any(AssetTag.class))).thenReturn(newTag);
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of());
        when(assetMapper.toDetail(any(), any(), any())).thenReturn(detail());

        assetService.updateMetadata(PRODUCT_ID, ASSET_ID, new UpdateAssetMetadataRequest("Nome", null, null, null, "blog, ,blog"));

        verify(assetTagRepository, org.mockito.Mockito.times(1)).save(any(AssetTag.class));
    }

    @Test
    void shouldListTags() {
        when(assetTagRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(new AssetTag(PRODUCT_ID, "blog")));

        assertThat(assetService.listTags(PRODUCT_ID)).containsExactly("blog");
    }

    @Test
    void shouldCreateTag() {
        when(assetTagRepository.existsByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(false);
        when(assetTagRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(new AssetTag(PRODUCT_ID, "blog")));

        List<String> result = assetService.createTag(PRODUCT_ID, "blog");

        assertThat(result).containsExactly("blog");
        verify(assetTagRepository).save(any(AssetTag.class));
    }

    @Test
    void shouldTrimTagNameOnCreate() {
        when(assetTagRepository.existsByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(false);
        when(assetTagRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of());

        assetService.createTag(PRODUCT_ID, "  blog  ");

        ArgumentCaptor<AssetTag> captor = ArgumentCaptor.forClass(AssetTag.class);
        verify(assetTagRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("blog");
    }

    @Test
    void shouldRejectBlankTagName() {
        assertThatThrownBy(() -> assetService.createTag(PRODUCT_ID, "   "))
                .isInstanceOf(InvalidAssetTagNameException.class);
        verify(assetTagRepository, never()).save(any());
    }

    @Test
    void shouldRejectNullTagName() {
        assertThatThrownBy(() -> assetService.createTag(PRODUCT_ID, null))
                .isInstanceOf(InvalidAssetTagNameException.class);
        verify(assetTagRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateTagName() {
        when(assetTagRepository.existsByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(true);

        assertThatThrownBy(() -> assetService.createTag(PRODUCT_ID, "blog"))
                .isInstanceOf(AssetTagAlreadyExistsException.class);
        verify(assetTagRepository, never()).save(any());
    }

    @Test
    void shouldDeleteTag() {
        AssetTag tag = new AssetTag(PRODUCT_ID, "blog");
        when(assetTagRepository.findByProductIdAndName(PRODUCT_ID, "blog")).thenReturn(Optional.of(tag));
        when(assetTagRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of());

        List<String> result = assetService.deleteTag(PRODUCT_ID, "blog");

        assertThat(result).isEmpty();
        verify(assetTagRepository).delete(tag);
    }

    @Test
    void shouldRejectDeletingMissingTag() {
        when(assetTagRepository.findByProductIdAndName(PRODUCT_ID, "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.deleteTag(PRODUCT_ID, "missing"))
                .isInstanceOf(AssetTagNotFoundException.class);
    }

    @Test
    void shouldRejectDeleteWithoutForceWhenAssetInUse() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(usageRepository.existsByAssetId(ASSET_ID)).thenReturn(true);

        assertThatThrownBy(() -> assetService.deleteAsset(caller, PRODUCT_ID, ASSET_ID, false))
                .isInstanceOf(AssetInUseException.class);
        verify(assetRepository, never()).delete(any());
        verify(auditService, never()).recordEvent(any());
    }

    @Test
    void shouldDeleteWithForceEvenWhenAssetInUse() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);

        assetService.deleteAsset(caller, PRODUCT_ID, ASSET_ID, true);

        verify(usageRepository, never()).existsByAssetId(any());
        verify(storageProvider).delete(asset.getStorageKey());
        verify(assetRepository).delete(asset);
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("ASSET_DELETED");
        assertThat(auditCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(auditCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(auditCaptor.getValue().targetId()).isEqualTo(ASSET_ID.toString());
    }

    @Test
    void shouldDeleteWithoutForceWhenAssetHasNoUsage() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        when(usageRepository.existsByAssetId(ASSET_ID)).thenReturn(false);
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);

        assetService.deleteAsset(caller, PRODUCT_ID, ASSET_ID, false);

        verify(assetRepository).delete(asset);
    }

    @Test
    void shouldListUsage() {
        Asset asset = asset();
        when(assetRepository.findByProductIdAndId(PRODUCT_ID, ASSET_ID)).thenReturn(Optional.of(asset));
        AssetUsage usage = new AssetUsage(ASSET_ID, "CONTENT", "content-1", "Artigo");
        when(usageRepository.findAllByAssetId(ASSET_ID)).thenReturn(List.of(usage));
        AssetUsageSummary summary = new AssetUsageSummary("CONTENT", "content-1", "Artigo");
        when(assetMapper.toUsageSummary(usage)).thenReturn(summary);

        assertThat(assetService.listUsage(PRODUCT_ID, ASSET_ID)).containsExactly(summary);
    }

    @Test
    void shouldResolveLocalAsset() {
        Asset asset = asset();
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.resolve(ASSET_ID, asset.getStorageKey()))
                .thenReturn(new ResolvedLocation("/api/v1/assets/" + ASSET_ID + "/file", null));

        ResolvedAsset resolved = assetService.resolveAsset(ASSET_ID, caller);

        assertThat(resolved.url()).isEqualTo("/api/v1/assets/" + ASSET_ID + "/file");
        assertThat(resolved.expiresAt()).isNull();
        assertThat(resolved.contentType()).isEqualTo(asset.getMimeType());
        verify(productAccessPort).assertAccessible(PRODUCT_ID, caller);
    }

    @Test
    void shouldResolveS3AssetWithExpiration() {
        Asset asset = asset(AssetStorageStrategy.S3);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(storageProvisioningService.resolveProvider(AssetStorageStrategy.S3)).thenReturn(storageProvider);
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-06-27T12:00:00Z");
        when(storageProvider.resolve(ASSET_ID, asset.getStorageKey()))
                .thenReturn(new ResolvedLocation("https://bucket.s3.amazonaws.com/key?sig=abc", expiresAt));

        ResolvedAsset resolved = assetService.resolveAsset(ASSET_ID, caller);

        assertThat(resolved.url()).isEqualTo("https://bucket.s3.amazonaws.com/key?sig=abc");
        assertThat(resolved.expiresAt()).isEqualTo(expiresAt.toString());
    }

    @Test
    void shouldReturn404ForCrossTenantResolveWithoutLeakingExistence() {
        Asset asset = asset();
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        org.mockito.Mockito.doThrow(new ProductNotFoundException(PRODUCT_ID))
                .when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        assertThatThrownBy(() -> assetService.resolveAsset(ASSET_ID, caller))
                .isInstanceOf(ProductNotFoundException.class);
        verify(storageProvisioningService, never()).resolveProvider(any());
    }

    @Test
    void shouldRejectResolveForMissingAsset() {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.resolveAsset(ASSET_ID, caller))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void shouldLoadLocalAssetFile() {
        Asset asset = asset();
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(localStorageProvider.loadContent(asset.getStorageKey())).thenReturn("conteudo".getBytes());

        AssetFileContent content = assetService.loadAssetFile(ASSET_ID, caller);

        assertThat(content.content()).isEqualTo("conteudo".getBytes());
        assertThat(content.contentType()).isEqualTo(asset.getMimeType());
        assertThat(content.filename()).isEqualTo(asset.getName());
    }

    @Test
    void shouldRejectLoadingFileForS3Asset() {
        Asset asset = asset(AssetStorageStrategy.S3);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.loadAssetFile(ASSET_ID, caller))
                .isInstanceOf(AssetNotFoundException.class);
        verify(localStorageProvider, never()).loadContent(any());
    }

    private Asset asset() {
        return asset(AssetStorageStrategy.LOCAL);
    }

    private Asset asset(AssetStorageStrategy strategy) {
        Asset asset = new Asset(new Asset.Creation(TENANT_ID, PRODUCT_ID, "foto.png", "image/png", AssetCategory.IMAGE, 1024L,
                strategy, "aegis/pms/x/y/image/foto.png", "subject-1"));
        ReflectionTestUtils.setField(asset, "id", ASSET_ID);
        return asset;
    }

    private AssetDetail detail() {
        return new AssetDetail(ASSET_ID, "foto.png", "foto.png", null, null, null, "image/png", "image", 1024L,
                "ativo", List.of(), List.of(), "subject-1", FIXED_TIMESTAMP, FIXED_TIMESTAMP);
    }
}
