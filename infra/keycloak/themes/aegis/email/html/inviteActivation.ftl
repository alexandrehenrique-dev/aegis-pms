<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Você foi convidado - Aegis PMS</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
    .container { max-width: 560px; margin: 40px auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
    .header { background: #1a1a2e; padding: 32px 40px; text-align: center; }
    .header h1 { color: #fff; font-size: 20px; margin: 0; font-weight: 600; }
    .body { padding: 36px 40px; color: #374151; }
    p { font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
    .invite-card { background: #ede9fe; border-radius: 8px; padding: 18px 22px; margin: 22px 0; }
    .invite-card .label { font-size: 11px; color: #7c3aed; text-transform: uppercase; letter-spacing: .5px; font-weight: 600; margin-bottom: 10px; }
    .invite-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; gap: 16px; }
    .invite-row .key { color: #6b7280; }
    .invite-row .val { font-weight: 600; color: #111827; text-align: right; }
    .cta { text-align: center; margin: 28px 0 12px; }
    .btn { display: inline-block; background: #4f46e5; color: #fff; text-decoration: none; padding: 14px 40px; border-radius: 7px; font-size: 15px; font-weight: 600; }
    .expire { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 10px; }
    .warning { background: #fffbeb; border: 1px solid #fcd34d; border-radius: 6px; padding: 12px 16px; font-size: 13px; color: #92400e; margin: 20px 0; }
    .footer { padding: 20px 40px; border-top: 1px solid #f3f4f6; color: #9ca3af; font-size: 12px; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Aegis PMS</h1></div>
    <div class="body">
      <p>Olá, <strong>${userName}</strong>.</p>
      <p><strong>${inviterName}</strong> convidou você para a plataforma <strong>Aegis PMS</strong>. Clique no botão abaixo para definir sua senha e ativar sua conta.</p>

      <div class="invite-card">
        <div class="label">Detalhes do convite</div>
        <div class="invite-row"><span class="key">Workspace</span><span class="val">${tenantName}</span></div>
        <div class="invite-row"><span class="key">Papel</span><span class="val">${role}</span></div>
        <div class="invite-row"><span class="key">Produtos</span><span class="val">${productNames}</span></div>
      </div>

      <div class="cta">
        <a href="${activationUrl}" class="btn">Ativar minha conta</a>
      </div>
      <div class="expire">Este link expira em <strong>${expiresAt}</strong>.</div>

      <div class="warning">
        Se você não esperava este convite, ignore este e-mail. Nenhuma ação é necessária da sua parte.
      </div>
    </div>
    <div class="footer">Aegis PMS · aegis.app<br>Você recebeu este e-mail porque foi convidado para a plataforma.</div>
  </div>
</body>
</html>
