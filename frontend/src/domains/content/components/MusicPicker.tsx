import { useState } from "react";
import { Music, Link as LinkIcon } from "lucide-react";
import { Button, Field } from "../../../shared/components/Primitives";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "../../../shared/components/ui/tabs";
import { AssetPickerModal } from "../../assets/components/AssetPickerModal";
import type { AssetSummary } from "../../assets/contracts/responses";

export type MusicSource = { type: "asset"; assetId: string; name: string } | { type: "spotify"; url: string };

function isValidSpotifyUrl(url: string): boolean {
  return /^https:\/\/open\.spotify\.com\/(track|album|playlist)\//.test(url.trim());
}

/**
 * J.2 (BUG-SPRINT-05) — substitui o `EntityPicker` genérico (nós de qualquer
 * tipo do Knowledge Graph) como ponto de entrada de "Vincular música": o
 * autor escolhe entre um asset de áudio já enviado ao produto ou uma URL do
 * Spotify (faixa/álbum/playlist). O nó `MUSIC_REF` no grafo é uma referência
 * secundária derivada dessa escolha, não o mecanismo de seleção em si.
 */
export function MusicPicker({ onSelect }: { onSelect: (src: MusicSource) => void }) {
  const [tab, setTab] = useState<"asset" | "spotify">("asset");
  const [pickerOpen, setPickerOpen] = useState(false);
  const [spotifyUrl, setSpotifyUrl] = useState("");
  const spotifyErr = spotifyUrl.trim() && !isValidSpotifyUrl(spotifyUrl)
    ? "URL do Spotify inválida — use um link de faixa, álbum ou playlist (open.spotify.com/...)."
    : undefined;

  const handleAssetSelected = (asset: AssetSummary) => {
    setPickerOpen(false);
    onSelect({ type: "asset", assetId: asset.id ?? asset.name, name: asset.name });
  };

  return (
    <div className="w-80">
      <Tabs value={tab} onValueChange={(v) => setTab(v as "asset" | "spotify")}>
        <TabsList className="w-full">
          <TabsTrigger value="asset" className="flex items-center gap-1.5"><Music size={13} />Asset</TabsTrigger>
          <TabsTrigger value="spotify" className="flex items-center gap-1.5"><LinkIcon size={13} />Spotify URL</TabsTrigger>
        </TabsList>
        <TabsContent value="asset" className="mt-2 space-y-2">
          <p className="text-xs text-muted-foreground">Selecione um arquivo de áudio já enviado aos Assets deste produto.</p>
          <Button className="flex items-center gap-1.5" onClick={() => setPickerOpen(true)}><Music size={14} />Selecionar áudio</Button>
          <AssetPickerModal open={pickerOpen} typeFilter="áudio" lockFilter onClose={() => setPickerOpen(false)} onSelect={handleAssetSelected} />
        </TabsContent>
        <TabsContent value="spotify" className="mt-2 space-y-2">
          <Field label="URL do Spotify" value={spotifyUrl} onChange={setSpotifyUrl} error={spotifyErr} />
          <Button className="flex items-center gap-1.5" primary disabled={!spotifyUrl.trim() || !!spotifyErr} onClick={() => onSelect({ type: "spotify", url: spotifyUrl.trim() })}>
            Confirmar
          </Button>
        </TabsContent>
      </Tabs>
    </div>
  );
}
