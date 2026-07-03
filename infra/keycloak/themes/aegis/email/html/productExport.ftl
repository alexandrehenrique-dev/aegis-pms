<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Exportação de dados — ${productName}</title>
</head>
<body style="font-family: Arial, sans-serif; background: #f5f5f5; padding: 24px;">
  <div style="max-width: 600px; margin: 0 auto; background: #ffffff; padding: 28px; border-radius: 8px;">
    <h1 style="font-size: 20px;">Aegis PMS</h1>
    <p>Olá, <strong>${userName}</strong>.</p>
    <p>A exportação do produto <strong>${productName}</strong> está pronta para download.</p>
    <p>Tenant: <strong>${tenantName}</strong></p>
    <p>Arquivo ZIP: <strong>${fileSizeMb} MB</strong></p>
    <ul>
      <li>Conteúdos: ${entityCounts.contentEntries}</li>
      <li>Páginas: ${entityCounts.pages}</li>
      <li>Formulários: ${entityCounts.forms}</li>
      <li>Submissões: ${entityCounts.formSubmissions}</li>
      <li>Assets: ${entityCounts.assets}</li>
      <li>Nós do Knowledge Graph: ${entityCounts.knowledgeGraphNodes}</li>
      <li>Eventos de auditoria: ${entityCounts.auditEvents}</li>
    </ul>
    <p>
      <a href="${downloadUrl}" style="display:inline-block;background:#4f46e5;color:#fff;padding:12px 20px;border-radius:6px;text-decoration:none;">
        Baixar dados
      </a>
    </p>
    <p>Este link expira em <strong>${expiresAt}</strong>. O ZIP não está anexado a este e-mail.</p>
  </div>
</body>
</html>
