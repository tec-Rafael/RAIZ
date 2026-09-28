package br.com.raiz.score.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_models")
public class AiModel {

    @Id
    @Column(name = "model_id", length = 64)
    private String modelId;

    @Column(name = "ai_tool_id", length = 64)
    private String aiToolId;

    @Column(name = "model_name", length = 255)
    private String modelName;

    @Column(name = "model_family", length = 255)
    private String modelFamily;

    @Column(name = "input_price_usd_per_1k_tokens")
    private Double inputPriceUsdPer1kTokens;

    @Column(name = "output_price_usd_per_1k_tokens")
    private Double outputPriceUsdPer1kTokens;

    protected AiModel() {
        // exigido pelo JPA
    }

    public AiModel(
            String modelId,
            String aiToolId,
            String modelName,
            String modelFamily,
            Double inputPriceUsdPer1kTokens,
            Double outputPriceUsdPer1kTokens) {

        this.modelId = modelId;
        this.aiToolId = aiToolId;
        this.modelName = modelName;
        this.modelFamily = modelFamily;
        this.inputPriceUsdPer1kTokens = inputPriceUsdPer1kTokens;
        this.outputPriceUsdPer1kTokens = outputPriceUsdPer1kTokens;
    }

    public String getModelId() {
        return modelId;
    }

    public String getAiToolId() {
        return aiToolId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelFamily() {
        return modelFamily;
    }

    public Double getInputPriceUsdPer1kTokens() {
        return inputPriceUsdPer1kTokens;
    }

    public Double getOutputPriceUsdPer1kTokens() {
        return outputPriceUsdPer1kTokens;
    }
}