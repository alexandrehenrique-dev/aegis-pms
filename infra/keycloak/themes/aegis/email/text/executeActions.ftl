Olá${firstName?has_content?then(', ' + firstName, '')}!

Você foi convidado para acessar o Aegis PMS.
Acesse o link abaixo para definir sua senha:

${link}

Este link expira em ${linkExpirationFormatter(linkExpiration)?no_esc}.

Se você não esperava este convite, ignore este e-mail.