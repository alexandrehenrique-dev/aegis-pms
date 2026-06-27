package br.com.byop.aegis.asset.controller;

import br.com.byop.aegis.asset.contract.DeleteAssetRequest;
import br.com.byop.aegis.asset.contract.UpdateAssetMetadataRequest;
import br.com.byop.aegis.asset.dto.AssetDetail;
import br.com.byop.aegis.asset.dto.AssetFileContent;
import br.com.byop.aegis.asset.dto.AssetSummary;
import br.com.byop.aegis.asset.dto.AssetUsageSummary;
import br.com.byop.aegis.asset.dto.ResolvedAsset;
import br.com.byop.aegis.asset.exception.InvalidAssetTagNameException;
import br.com.byop.aegis.asset.service.AssetService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@RestController
public class AssetController {

    private final AssetService assetService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ObjectMapper objectMapper;

    public AssetController(AssetService assetService, ProductAccessPort productAccessPort,
                           AuthenticatedUserProvider authenticatedUserProvider, ObjectMapper objectMapper) {
        this.assetService = assetService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/v1/products/{productId}/assets")
    @RequireModule(ModuleKey.ASSETS)
    public List<AssetSummary> listAssets(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.listAssets(productId);
    }

    @PostMapping(value = "/api/v1/products/{productId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequireModule(ModuleKey.ASSETS)
    public AssetSummary uploadAsset(@PathVariable("productId") UUID productId,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "friendlyName", required = false) String friendlyName,
                                    Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return assetService.uploadAsset(productId, file, friendlyName, caller);
    }

    @GetMapping("/api/v1/products/{productId}/assets/{assetId}")
    @RequireModule(ModuleKey.ASSETS)
    public AssetDetail getAsset(@PathVariable("productId") UUID productId,
                                @PathVariable("assetId") UUID assetId,
                                Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.getAsset(productId, assetId);
    }

    @PutMapping("/api/v1/products/{productId}/assets/{assetId}/metadata")
    @RequireModule(ModuleKey.ASSETS)
    public AssetDetail updateMetadata(@PathVariable("productId") UUID productId,
                                      @PathVariable("assetId") UUID assetId,
                                      @Valid @RequestBody UpdateAssetMetadataRequest request,
                                      Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.updateMetadata(productId, assetId, request);
    }

    @DeleteMapping("/api/v1/products/{productId}/assets/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireModule(ModuleKey.ASSETS)
    public void deleteAsset(@PathVariable("productId") UUID productId,
                            @PathVariable("assetId") UUID assetId,
                            @RequestBody(required = false) DeleteAssetRequest request,
                            Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        boolean force = request != null && request.force();
        assetService.deleteAsset(caller, productId, assetId, force);
    }

    @GetMapping("/api/v1/products/{productId}/assets/{assetId}/usage")
    @RequireModule(ModuleKey.ASSETS)
    public List<AssetUsageSummary> listUsage(@PathVariable("productId") UUID productId,
                                             @PathVariable("assetId") UUID assetId,
                                             Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.listUsage(productId, assetId);
    }

    @GetMapping("/api/v1/products/{productId}/asset-tags")
    @RequireModule(ModuleKey.ASSETS)
    public List<String> listTags(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.listTags(productId);
    }

    @PostMapping("/api/v1/products/{productId}/asset-tags")
    @RequireModule(ModuleKey.ASSETS)
    public List<String> createTag(@PathVariable("productId") UUID productId,
                                  @RequestBody String rawName,
                                  Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.createTag(productId, decodeJsonStringBody(rawName));
    }

    @DeleteMapping("/api/v1/products/{productId}/asset-tags/{tag}")
    @RequireModule(ModuleKey.ASSETS)
    public List<String> deleteTag(@PathVariable("productId") UUID productId,
                                  @PathVariable("tag") String tag,
                                  Authentication authentication) {
        assertProductAccess(authentication, productId);
        return assetService.deleteTag(productId, tag);
    }

    @GetMapping("/api/v1/assets/{assetId}/resolve")
    public ResolvedAsset resolveAsset(@PathVariable("assetId") UUID assetId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return assetService.resolveAsset(assetId, caller);
    }

    @GetMapping("/api/v1/assets/{assetId}/file")
    public ResponseEntity<byte[]> downloadAssetFile(@PathVariable("assetId") UUID assetId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        AssetFileContent file = assetService.loadAssetFile(assetId, caller);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }

    /**
     * O corpo de {@code POST .../asset-tags} e um JSON string literal
     * (ex.: {@code "institucional"}), nao um objeto — o conversor padrao de
     * {@code String} do Spring MVC entrega o corpo bruto, com as aspas
     * inclusas; decodificar explicitamente via Jackson evita gravar a tag
     * com as aspas literais no nome.
     */
    private String decodeJsonStringBody(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, String.class);
        } catch (tools.jackson.core.JacksonException _) {
            throw new InvalidAssetTagNameException();
        }
    }
}
