<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Convite Aegis PMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f5;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
               style="background-color:#ffffff;border-radius:8px;overflow:hidden;
                      box-shadow:0 1px 3px rgba(0,0,0,0.1);">

          <!-- Header -->
          <tr>
            <td style="background-color:#1a1a2e;padding:32px 40px;text-align:center;">
              <span style="color:#ffffff;font-size:22px;font-weight:700;
                           letter-spacing:-0.5px;">Aegis PMS</span>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding:40px 40px 32px;">
              <p style="margin:0 0 16px;color:#111827;font-size:16px;font-weight:600;">
                Olá${firstName?has_content?then(', ' + firstName, '')}!
              </p>
              <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                Você foi convidado para acessar o <strong>Aegis PMS</strong>.
                Clique no botão abaixo para definir sua senha e ativar seu acesso.
              </p>

              <!-- CTA Button -->
              <table cellpadding="0" cellspacing="0" style="margin:0 0 32px;">
                <tr>
                  <td style="background-color:#4f46e5;border-radius:6px;">
                    <a href="${link}"
                       style="display:inline-block;padding:14px 28px;
                              color:#ffffff;font-size:15px;font-weight:600;
                              text-decoration:none;letter-spacing:0.2px;">
                      Definir minha senha
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0 0 8px;color:#6b7280;font-size:13px;line-height:1.5;">
                Se o botão não funcionar, copie e cole este link no seu navegador:
              </p>
              <p style="margin:0 0 24px;word-break:break-all;">
                <a href="${link}" style="color:#4f46e5;font-size:13px;">${link}</a>
              </p>

              <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.5;">
                Este link expira em <strong>${linkExpirationFormatter(linkExpiration)?no_esc}</strong>.
                Se você não esperava este convite, pode ignorar este e-mail com segurança.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color:#f9fafb;border-top:1px solid #e5e7eb;
                       padding:20px 40px;text-align:center;">
              <p style="margin:0;color:#9ca3af;font-size:12px;">
                Aegis PMS · Sistema de Gestão de Produtos
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>