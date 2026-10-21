-- Precos estimados e proxies documentados no Relatorio AMS.
-- Valores em USD por 1.000 tokens.

UPDATE ai_models
SET
    input_price_usd_per_1k_tokens = CASE model_id
                                        WHEN 'M08' THEN 0.00175
                                        WHEN 'M09' THEN 0.0178
                                        WHEN 'M10' THEN 0.0257
                                        WHEN 'M12' THEN 0.0319
                                        WHEN 'M25' THEN 0.025
        END,
    output_price_usd_per_1k_tokens = CASE model_id
                                         WHEN 'M08' THEN 0.014
                                         WHEN 'M09' THEN 0.142
                                         WHEN 'M10' THEN 0.205
                                         WHEN 'M12' THEN 0.255
                                         WHEN 'M25' THEN 0.20
        END
WHERE model_id IN ('M08', 'M09', 'M10', 'M12', 'M25');