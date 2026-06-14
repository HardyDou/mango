-- Restore the public Fuiou demo merchant signing key for the built-in Mango
-- test contract. The key pair belongs to Fuiou's public demo merchant
-- 08A9999999 / 0002900F0370542 and is not a production credential.
UPDATE `payment_channel_contract`
SET `config_values_json` = JSON_SET(
    COALESCE(`config_values_json`, JSON_OBJECT()),
    '$.privateKey',
    CONCAT(
      'MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJgAzD8fEvBHQTyxUEeK963mjziM',
      'WG7nxpi+pDMdtWiakc6xVhhbaipLaHo4wVI92A2wr3ptGQ1/YsASEHm3m2wGOpT2vrb2Ln/S7lz1',
      'ShjTKaT8U6rKgCdpQNHUuLhBQlpJer2mcYEzG/nGzcyalOCgXC/6CySiJCWJmPyR45bJAgMBAAEC',
      'gYBHFfBvAKBBwIEQ2jeaDbKBIFcQcgoVa81jt5xgz178WXUg/awu3emLeBKXPh2i0YtN87hM/+J8',
      'fnt3KbuMwMItCsTD72XFXLM4FgzJ4555CUCXBf5/tcKpS2xT8qV8QDr8oLKA18sQxWp8BMPrNp0e',
      'pmwun/gwgxoyQrJUB5YgZQJBAOiVXHiTnc3KwvIkdOEPmlfePFnkD4zzcv2UwTlHWgCyM/L8SCAF',
      'clXmSiJfKSZZS7o0kIeJJ6xe3Mf4/HSlhdMCQQCnTow+TnlEhDTPtWa+TUgzOys83Q/VLikqKmDz',
      'kWJ7I12+WX6AbxxEHLD+THn0JGrlvzTEIZyCe0sjQy4LzQNzAkEAr2SjfVJkuGJlrNENSwPHMugm',
      'vusbRwH3/38ET7udBdVdE6poga1Z0al+0njMwVypnNwy+eLWhkhrWmpLh3OjfQJAI3BV8JS6xzKh',
      '5SVtn/3Kv19XJ0tEIUnn2lCjvLQdAixZnQpj61ydxie1rggRBQ/5vLSlvq3H8zOelNeUF1fT1QJA',
      'DNo+tkHVXLY9H2kdWFoYTvuLexHAgrsnHxONOlSA5hcVLd1B3p9utOt3QeDf6x2i1lqhTH2w8gzj',
      'vsnx13tWqg=='
    )
  ),
  `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `contract_code` = 'FUIOU_PAY_MANGO_TECH'
  AND `merchant_no` = '0002900F0370542'
  AND `del_flag` = 0
  AND (
    JSON_UNQUOTE(JSON_EXTRACT(`config_values_json`, '$.privateKey')) IS NULL
    OR JSON_UNQUOTE(JSON_EXTRACT(`config_values_json`, '$.privateKey')) = ''
  );
