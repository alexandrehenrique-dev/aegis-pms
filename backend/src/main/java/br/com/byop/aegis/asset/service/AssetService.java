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
import br.com.byop.aegis.asset.exception.AssetTagAlreadyExistsException;
import br.com.byop.aegis.asset.exception.AssetTagNotFoundException;
import br.com.byop.aegis.asset.exception.InvalidAssetFilenameException;
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
import br.com.byop.aegis.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssetService {

    private static final String TARGET_TYPE_ASSET = "Asset";
    private static final String MODULE_ASSETS = "ASSETS";

    private final AssetRepository assetRepository;
    private final AssetTagRepository assetTagRepository;
    private final AssetTagAssignmentRepository assignmentRepository;
    private final AssetUsageRepository usageRepository;
    private final AssetMapper assetMapper;
    private final AssetUploadValidator uploadValidator;
    private final AssetStorageProvisioningService storageProvisioningService;
    private final LocalStorageProvider localStorageProvider;
    private final ProductReferenceService productReferenceService;
    private final ProductAccessPort productAccessPort;
    private final AuditService auditService;

    public AssetService(AssetRepository assetRepository, AssetTagRepository assetTagRepository,
                        AssetTagAssignmentRepository assignmentRepository, AssetUsageRepository usageRepository,
                        AssetMapper assetMapper, AssetUploadValidator uploadValidator,
                        AssetStorageProvisioningService storageProvisioningService, LocalStorageProvider localStorageProvider,
                        ProductReferenceService productReferenceService, ProductAccessPort productAccessPort,
                        AuditService auditService) {
        this.assetRepository = assetRepository;
        this.assetTagRepository = assetTagRepository;
        this.assignmentRepository = assignmentRepository;
        this.usageRepository = usageRepository;
        this.assetMapper = assetMapper;
        this.uploadValidator = uploadValidator;
        this.storageProvisioningService = storageProvisioningService;
        this.localStorageProvider = localStorageProvider;
        this.productReferenceService = productReferenceService;
        this.productAccessPort = productAccessPort;
        this.auditService = auditService;
    }

    @Transactional
    public AssetSummary uploadAsset(UUID productId, MultipartFile file, String friendlyName, AuthenticatedUser caller) {
        log.debug("uploadAsset: productId='{}', friendlyName='{}'", productId, friendlyName);
        ProductReference product = productReferenceService.getRequiredReference(productId);
        AssetStorageStrategy strategy = productReferenceService.getRequiredAssetStorageStrategy(productId);
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("uploadAsset: nome de arquivo invalido para productId='{}'", productId);
            throw new InvalidAssetFilenameException(originalFilename);
        }
        String mimeType = file.getContentType() == null ? "" : file.getContentType();
        AssetCategory category = uploadValidator.validate(mimeType, file.getSize());

        StorageProvider provider = storageProvisioningService.resolveProvider(strategy);
        String storageKey = provider.store(product.tenantId(), productId, category, originalFilename, readBytes(file));

        Asset asset = new Asset(new Asset.Creation(product.tenantId(), productId, originalFilename, mimeType,
                category, file.getSize(), strategy, storageKey, caller.subject()));
        if (friendlyName != null && !friendlyName.isBlank()) {
            asset.applyMetadata(new Asset.Metadata(friendlyName, null, null, null));
        }
        assetRepository.save(asset);
        log.info("uploadAsset: asset criado id='{}', productId='{}'", asset.getId(), productId);

        return toSummary(asset);
    }

    @Transactional(readOnly = true)
    public List<AssetSummary> listAssets(UUID productId) {
        log.debug("listAssets: productId='{}'", productId);
        return assetRepository.findAllByProductId(productId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetDetail getAsset(UUID productId, UUID assetId) {
        log.debug("getAsset: productId='{}', assetId='{}'", productId, assetId);
        return toDetail(findAssetInProduct(productId, assetId));
    }

    @Transactional
    public AssetDetail updateMetadata(UUID productId, UUID assetId, UpdateAssetMetadataRequest request) {
        log.debug("updateMetadata: productId='{}', assetId='{}'", productId, assetId);
        Asset asset = findAssetInProduct(productId, assetId);
        asset.applyMetadata(new Asset.Metadata(request.friendlyName(), request.altText(), request.caption(), request.credit()));
        assetRepository.save(asset);
        reconcileTags(productId, asset.getId(), parseCsv(request.tags()));
        log.info("updateMetadata: asset atualizado id='{}'", asset.getId());
        return toDetail(asset);
    }

    @Transactional
    public void deleteAsset(AuthenticatedUser caller, UUID productId, UUID assetId, boolean force) {
        log.debug("deleteAsset: productId='{}', assetId='{}', force='{}'", productId, assetId, force);
        Asset asset = findAssetInProduct(productId, assetId);
        if (!force && usageRepository.existsByAssetId(assetId)) {
            log.warn("deleteAsset: asset em uso, exclusao rejeitada id='{}'", assetId);
            throw new AssetInUseException(assetId);
        }
        storageProvisioningService.resolveProvider(asset.getStorageProvider()).delete(asset.getStorageKey());
        assetRepository.delete(asset);
        recordAssetDeletionAudit(caller, asset);
        log.info("deleteAsset: asset excluido id='{}'", assetId);
    }

    private void recordAssetDeletionAudit(AuthenticatedUser caller, Asset asset) {
        auditService.recordEvent(new AuditRecordCommand(
                asset.getTenantId(), asset.getProductId(), caller.subject(), "ASSET_DELETED", TARGET_TYPE_ASSET,
                asset.getId().toString(), asset.getName(), MODULE_ASSETS,
                Map.of("name", asset.getName(), "mimeType", asset.getMimeType()), null
        ));
    }

    @Transactional(readOnly = true)
    public List<AssetUsageSummary> listUsage(UUID productId, UUID assetId) {
        log.debug("listUsage: productId='{}', assetId='{}'", productId, assetId);
        findAssetInProduct(productId, assetId);
        return usageRepository.findAllByAssetId(assetId).stream()
                .map(assetMapper::toUsageSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listTags(UUID productId) {
        log.debug("listTags: productId='{}'", productId);
        return listTagNames(productId);
    }

    @Transactional
    public List<String> createTag(UUID productId, String name) {
        log.debug("createTag: productId='{}', name='{}'", productId, name);
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isBlank()) {
            log.warn("createTag: nome de tag em branco rejeitado para productId='{}'", productId);
            throw new InvalidAssetTagNameException();
        }
        if (assetTagRepository.existsByProductIdAndName(productId, trimmed)) {
            log.warn("createTag: tag ja existente rejeitada productId='{}', name='{}'", productId, trimmed);
            throw new AssetTagAlreadyExistsException(productId, trimmed);
        }
        assetTagRepository.save(new AssetTag(productId, trimmed));
        log.info("createTag: tag criada productId='{}', name='{}'", productId, trimmed);
        return listTagNames(productId);
    }

    @Transactional
    public List<String> deleteTag(UUID productId, String name) {
        log.debug("deleteTag: productId='{}', name='{}'", productId, name);
        AssetTag tag = assetTagRepository.findByProductIdAndName(productId, name)
                .orElseThrow(() -> new AssetTagNotFoundException(name));
        assetTagRepository.delete(tag);
        log.info("deleteTag: tag removida productId='{}', name='{}'", productId, name);
        return listTagNames(productId);
    }

    /**
     * Nucleo nao-transacional de {@link #listTags}/{@link #createTag}/{@link #deleteTag}
     * — chamado dentro da transacao já aberta pelo metodo publico chamador,
     * nunca via {@code this} a partir de outro metodo {@code @Transactional}
     * desta classe, para nao contornar o proxy do Spring (java:S6809).
     */
    private List<String> listTagNames(UUID productId) {
        return assetTagRepository.findAllByProductId(productId).stream()
                .map(AssetTag::getName)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResolvedAsset resolveAsset(UUID assetId, AuthenticatedUser caller) {
        log.debug("resolveAsset: assetId='{}', caller='{}'", assetId, caller.subject());
        Asset asset = requireAsset(assetId);
        productAccessPort.assertAccessible(asset.getProductId(), caller);
        ResolvedLocation location = storageProvisioningService.resolveProvider(asset.getStorageProvider())
                .resolve(asset.getId(), asset.getStorageKey());
        String expiresAt = location.expiresAt() == null ? null : location.expiresAt().toString();
        return new ResolvedAsset(asset.getId(), location.url(), expiresAt, asset.getMimeType());
    }

    @Transactional(readOnly = true)
    public AssetFileContent loadAssetFile(UUID assetId, AuthenticatedUser caller) {
        log.debug("loadAssetFile: assetId='{}', caller='{}'", assetId, caller.subject());
        Asset asset = requireAsset(assetId);
        productAccessPort.assertAccessible(asset.getProductId(), caller);
        if (asset.getStorageProvider() != AssetStorageStrategy.LOCAL) {
            log.warn("loadAssetFile: asset nao esta em storage LOCAL, rejeitado id='{}'", assetId);
            throw new AssetNotFoundException(assetId);
        }
        byte[] content = localStorageProvider.loadContent(asset.getStorageKey());
        return new AssetFileContent(content, asset.getMimeType(), asset.getName());
    }

    private Asset requireAsset(UUID assetId) {
        return assetRepository.findById(assetId).orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    private Asset findAssetInProduct(UUID productId, UUID assetId) {
        return assetRepository.findByProductIdAndId(productId, assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    private AssetSummary toSummary(Asset asset) {
        List<String> tagNames = tagNamesFor(asset.getId());
        List<String> usageLabels = usageRepository.findAllByAssetId(asset.getId()).stream()
                .map(AssetUsage::getUsedInLabel)
                .toList();
        return assetMapper.toSummary(asset, tagNames, usageLabels);
    }

    private AssetDetail toDetail(Asset asset) {
        List<String> tagNames = tagNamesFor(asset.getId());
        List<AssetUsageSummary> usages = usageRepository.findAllByAssetId(asset.getId()).stream()
                .map(assetMapper::toUsageSummary)
                .toList();
        return assetMapper.toDetail(asset, tagNames, usages);
    }

    private List<String> tagNamesFor(UUID assetId) {
        List<UUID> tagIds = assignmentRepository.findAllByAssetId(assetId).stream()
                .map(AssetTagAssignment::getAssetTagId)
                .toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        return assetTagRepository.findAllByIdIn(tagIds).stream()
                .map(AssetTag::getName)
                .toList();
    }

    private void reconcileTags(UUID productId, UUID assetId, List<String> desiredNames) {
        List<AssetTagAssignment> currentAssignments = assignmentRepository.findAllByAssetId(assetId);
        Map<UUID, AssetTag> currentTagsById = assetTagRepository.findAllByIdIn(
                currentAssignments.stream().map(AssetTagAssignment::getAssetTagId).toList()
        ).stream().collect(Collectors.toMap(AssetTag::getId, tag -> tag));

        Set<String> desired = new LinkedHashSet<>(desiredNames);

        for (AssetTagAssignment assignment : currentAssignments) {
            AssetTag tag = currentTagsById.get(assignment.getAssetTagId());
            if (tag == null || !desired.contains(tag.getName())) {
                assignmentRepository.deleteByAssetIdAndAssetTagId(assetId, assignment.getAssetTagId());
            }
        }

        Set<String> currentNames = currentTagsById.values().stream().map(AssetTag::getName).collect(Collectors.toSet());
        for (String name : desired) {
            if (!currentNames.contains(name)) {
                assignTag(productId, assetId, name);
            }
        }
    }

    private void assignTag(UUID productId, UUID assetId, String name) {
        AssetTag tag = assetTagRepository.findByProductIdAndName(productId, name)
                .orElseGet(() -> assetTagRepository.save(new AssetTag(productId, name)));
        if (!assignmentRepository.existsByAssetIdAndAssetTagId(assetId, tag.getId())) {
            assignmentRepository.save(new AssetTagAssignment(assetId, tag.getId()));
        }
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read uploaded file content", exception);
        }
    }
}
