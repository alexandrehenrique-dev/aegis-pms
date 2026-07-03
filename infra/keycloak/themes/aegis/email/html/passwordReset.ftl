<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Recuperação de senha - Aegis PMS</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
    .container { max-width: 560px; margin: 40px auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
    .header { background: #1a1a2e; padding: 32px 40px; text-align: center; }
    .header h1 { color: #fff; font-size: 20px; margin: 0; font-weight: 600; }
    .body { padding: 36px 40px; color: #374151; }
    p { font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
    .cta { text-align: center; margin: 28px 0 12px; }
    .btn { display: inline-block; background: #4f46e5; color: #fff; text-decoration: none; padding: 14px 40px; border-radius: 7px; font-size: 15px; font-weight: 600; }
    .expire { font-size: 12px; color: #9ca3af; text-align: center; margin-top: 10px; }
    .security { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 14px 18px; font-size: 13px; color: #6b7280; margin: 20px 0; line-height: 1.5; }
    .footer { padding: 20px 40px; border-top: 1px solid #f3f4f6; color: #9ca3af; font-size: 12px; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Aegis PMS</h1></div>
    <div class="body">
      <p>Olá, <strong>${userName}</strong>.</p>
      <p>Recebemos uma solicitação para redefinir a senha da sua conta. Clique no botão abaixo para criar uma nova senha.</p>

      <div class="cta">
        <a href="${resetUrl}" class="btn">Redefinir minha senha</a>
      </div>
      <div class="expire">Este link é válido por <strong>1 hora</strong> e expira em <strong>${expiresAt}</strong>.</div>

      <div class="security">
        Se você não solicitou a recuperação de senha, ignore este e-mail. Sua senha permanece a mesma e nenhuma ação é necessária.
      </div>
    </div>
    <div class="footer">Aegis PMS · aegis.app<br>Este e-mail foi enviado porque alguém solicitou a recuperação de senha desta conta.</div>
  </div>
</body>
</html>
