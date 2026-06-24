Olá${firstName?has_content?then(', ' + firstName, '')}!

Recebemos uma solicitação para redefinir sua senha no Aegis PMS.
Acesse o link abaixo para criar uma nova senha:

${link}

Este link expira em ${linkExpirationFormatter(linkExpiration)?no_esc}.

Se você não solicitou isso, ignore este e-mail.