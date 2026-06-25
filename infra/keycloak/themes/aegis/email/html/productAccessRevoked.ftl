<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Seu acesso a um produto foi removido</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 0; }
    .container { max-width: 560px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .header { background: #1a1a2e; padding: 32px 40px; text-align: center; }
    .header h1 { color: #ffffff; font-size: 20px; margin: 0; font-weight: 600; letter-spacing: -0.3px; }
    .body { padding: 40px; color: #374151; }
    .body p { font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
    .product-card { background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; padding: 16px 20px; margin: 24px 0; }
    .product-card .label { font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
    .product-card .value { font-size: 16px; font-weight: 600; color: #111827; }
    .footer { padding: 24px 40px; border-top: 1px solid #f3f4f6; color: #9ca3af; font-size: 12px; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Aegis PMS</h1>
    </div>
    <div class="body">
      <p>Olá, <strong>${userName}</strong>.</p>
      <p>Seu acesso ao seguinte produto em <strong>${tenantName}</strong> foi removido:</p>
      <div class="product-card">
        <div class="label">Produto</div>
        <div class="value">${productName}</div>
      </div>
      <p>Se você acredita que isto foi um engano, entre em contato com o administrador do seu workspace.</p>
    </div>
    <div class="footer">
      Você recebeu este e-mail porque um administrador removeu seu acesso a este produto.
    </div>
  </div>
</body>
</html>
