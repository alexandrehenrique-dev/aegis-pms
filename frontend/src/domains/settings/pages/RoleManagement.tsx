import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { RiskBadge } from "../../../shared/components/RiskBadge";

function RoleCard({ r }: { r: string[] }) {
  return (
    <Card>
      <div className="flex justify-between"><h3 className="font-semibold">{r[0]}</h3><RiskBadge risk={r[3]} /></div>
      <p className="mt-2 text-sm text-muted-foreground">{r[1]}</p>
      <p className="mt-3 text-sm">Usuários: <b>{r[2]}</b></p>
      <p className="text-xs text-muted-foreground">Última alteração: {r[4]}</p>
      <div className="mt-4 flex gap-2"><Button>Editar</Button><Button>Impacto</Button></div>
    </Card>
  );
}

export function RoleManagement() {
  const roles = [
    ["Super Admin", "Acesso global ao sistema", "1", "alto", "30 dias"],
    ["Tenant Admin", "Administra tenant e equipe", "2", "alto", "2 dias"],
    ["Product Manager", "Opera produto e aprova fluxos", "4", "médio", "ontem"],
    ["Editor", "Cria e revisa conteúdo", "8", "médio", "2 dias"],
    ["Viewer", "Somente leitura", "6", "baixo", "padrão"],
  ];
  return (
    <>
      <PageHeader title="Role Management" module="Permissions" desc="Gerencie papéis, risco e permissões principais." badge="Roles">
        <Button>Restaurar padrão</Button>
        <Button primary>Criar role</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">{roles.map((r) => <RoleCard key={r[0]} r={r} />)}</div>
    </>
  );
}
