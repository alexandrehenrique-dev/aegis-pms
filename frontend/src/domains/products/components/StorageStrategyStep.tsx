import { Badge, Field } from "../../../shared/components/Primitives";
import type { AssetStorageStrategy } from "../contracts/requests";

/** Passo "Armazenamento de assets" — compartilhado por `CreateProductForm` e `CreateProductModal` (ver `useModuleSelection` para o mesmo princípio aplicado aos módulos). */
export function StorageStrategyStep({ strategy, onChange, s3Bucket, onChangeBucket, s3Region, onChangeRegion }: {
  strategy: AssetStorageStrategy; onChange: (s: AssetStorageStrategy) => void;
  s3Bucket: string; onChangeBucket: (v: string) => void; s3Region: string; onChangeRegion: (v: string) => void;
}) {
  return (
    <div>
      <p className="mb-2 text-sm font-medium">Armazenamento de assets</p>
      <div className="grid gap-3 md:grid-cols-2">
        <button
          type="button"
          onClick={() => onChange("local")}
          className={`rounded-xl border p-3 text-left text-sm ${strategy === "local" ? "border-primary bg-muted" : "border-border"}`}
        >
          <p className="font-medium">Local <Badge tone="green">recomendado</Badge></p>
          <p className="mt-1 text-xs text-muted-foreground">Os arquivos do produto ficam guardados no próprio servidor.</p>
        </button>
        <button
          type="button"
          onClick={() => onChange("s3")}
          className={`rounded-xl border p-3 text-left text-sm ${strategy === "s3" ? "border-primary bg-muted" : "border-border"}`}
        >
          <p className="font-medium">Bucket externo (S3)</p>
          <p className="mt-1 text-xs text-muted-foreground">Para quem já usa ou vai usar um provedor de nuvem próprio — exige configuração adicional.</p>
        </button>
      </div>
      {strategy === "s3" && (
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <Field label="Bucket (opcional, configurável depois)" value={s3Bucket} onChange={onChangeBucket} />
          <Field label="Região (opcional, configurável depois)" value={s3Region} onChange={onChangeRegion} />
        </div>
      )}
    </div>
  );
}
