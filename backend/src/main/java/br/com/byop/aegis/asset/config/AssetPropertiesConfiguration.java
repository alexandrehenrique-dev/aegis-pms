package br.com.byop.aegis.asset.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AssetLimitsProperties.class, S3StorageProperties.class})
class AssetPropertiesConfiguration {
}
