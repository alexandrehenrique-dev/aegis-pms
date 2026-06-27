package br.com.byop.aegis.asset.mapper;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.domain.AssetStatus;
import br.com.byop.aegis.asset.domain.AssetUsage;
import br.com.byop.aegis.asset.dto.AssetDetail;
import br.com.byop.aegis.asset.dto.AssetSummary;
import br.com.byop.aegis.asset.dto.AssetUsageSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    String EMPTY_LIST_PLACEHOLDER = "";

    @Mapping(target = "name", source = "asset.friendlyName")
    @Mapping(target = "type", expression = "java(asset == null ? null : toCategoryContractValue(asset.getCategory()))")
    @Mapping(target = "size", expression = "java(asset == null ? null : toFormattedSize(asset.getSizeBytes()))")
    @Mapping(target = "status", expression = "java(asset == null ? null : toStatusContractValue(asset.getStatus()))")
    @Mapping(target = "tags", source = "tagNames", qualifiedByName = "toCsv")
    @Mapping(target = "usage", source = "usageLabels", qualifiedByName = "toCsv")
    @Mapping(target = "uploadedAt", expression = "java(asset == null ? null : asset.getCreatedAt().toLocalDate().toString())")
    AssetSummary toSummary(Asset asset, List<String> tagNames, List<String> usageLabels);

    @Mapping(target = "category", expression = "java(asset == null ? null : toCategoryContractValue(asset.getCategory()))")
    @Mapping(target = "status", expression = "java(asset == null ? null : toStatusContractValue(asset.getStatus()))")
    @Mapping(target = "tags", source = "tagNames")
    @Mapping(target = "usage", source = "usages")
    AssetDetail toDetail(Asset asset, List<String> tagNames, List<AssetUsageSummary> usages);

    AssetUsageSummary toUsageSummary(AssetUsage usage);

    @Named("toCategoryContractValue")
    default String toCategoryContractValue(AssetCategory category) {
        return category == null ? null : category.contractValue();
    }

    @Named("toStatusContractValue")
    default String toStatusContractValue(AssetStatus status) {
        return status == null ? null : status.contractValue();
    }

    @Named("toCsv")
    default String toCsv(List<String> values) {
        return values == null || values.isEmpty() ? EMPTY_LIST_PLACEHOLDER : String.join(", ", values);
    }

    @Named("toFormattedSize")
    default String toFormattedSize(long sizeBytes) {
        if (sizeBytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
        return Math.max(1, Math.round(sizeBytes / 1024.0)) + " KB";
    }
}
