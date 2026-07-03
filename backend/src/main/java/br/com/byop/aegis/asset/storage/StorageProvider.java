package br.com.byop.aegis.asset.storage;

import br.com.byop.aegis.asset.domain.AssetCategory;

import java.io.InputStream;
import java.util.UUID;

/**
 * Abstrai onde os bytes de um {@link br.com.byop.aegis.asset.domain.Asset}
 * sao armazenados, conforme {@link br.com.byop.aegis.product.api.AssetStorageStrategy}
 * escolhida pelo produto no momento do upload. Duas implementacoes: {@link LocalStorageProvider}
 * e {@link S3StorageProvider}.
 */
public interface StorageProvider {

    /**
     * Garante que a estrutura de pastas do produto exista antes do primeiro
     * upload — uma subpasta por {@link AssetCategory}. No-op para provedores
     * que nao usam pastas (ex.: S3, isolado so pelo prefixo da chave).
     *
     * @param tenantId identificador do tenant proprietario do produto
     * @param productId identificador do produto
     */
    void provisionProductFolders(UUID tenantId, UUID productId);

    /**
     * Grava o conteudo de um arquivo e devolve o {@code storageKey} relativo
     * (nunca um caminho absoluto de disco nem uma URL pronta).
     *
     * @param tenantId identificador do tenant proprietario do produto
     * @param productId identificador do produto
     * @param category categoria do asset, usada para a subpasta/prefixo
     * @param originalFilename nome original do arquivo enviado pelo cliente, sanitizado antes do uso
     * @param content bytes do arquivo
     * @return chave relativa de armazenamento (ex.: {@code aegis/pms/{tenantId}/{productId}/{category}/{filename}})
     */
    String store(UUID tenantId, UUID productId, AssetCategory category, String originalFilename, byte[] content);

    /**
     * Resolve a URL de uso de um asset ja armazenado — nunca expõe caminho de
     * disco nem credencial. Para {@code s3}, gera uma URL pre-assinada com TTL curto.
     *
     * @param assetId identificador do asset (usado pelo provedor local para montar a URL servida pelo backend)
     * @param storageKey chave de armazenamento retornada por {@link #store}
     * @return localizacao resolvida, com {@code expiresAt} preenchido apenas quando aplicavel
     */
    ResolvedLocation resolve(UUID assetId, String storageKey);

    /**
     * Abre o conteudo do asset para leitura em streaming. Chamadores devem
     * fechar o stream retornado.
     *
     * @param storageKey chave de armazenamento retornada por {@link #store}
     * @return stream de leitura do asset
     */
    InputStream openStream(String storageKey);

    /**
     * Remove o conteudo associado a um {@code storageKey}.
     *
     * @param storageKey chave de armazenamento retornada por {@link #store}
     */
    void delete(String storageKey);
}
