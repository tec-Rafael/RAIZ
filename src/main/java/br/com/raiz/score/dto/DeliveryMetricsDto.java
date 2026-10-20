package br.com.raiz.score.dto;

public class DeliveryMetricsDto {

    private final String employeeId;
    private final String jobRole;

    private final Double issueResolutionTime;
    private final Double mergeTime;
    private final Double reviewRate;
    private final Integer prVolume;
    private final Integer reviewedPrCount;

    private final Double peerMean;
    private final Double adjustedReviewRate;

    private final Double relativeIssueScore;
    private final Double relativeMergeScore;
    private final Double relativeReviewScore;
    private final Double relativePrScore;

    private final Integer totalFuncionariosIssue;
    private final Integer totalFuncionariosMerge;
    private final Integer totalFuncionariosReview;
    private final Integer totalFuncionariosPr;

    private final Double individualIssueScore;
    private final Double individualMergeScore;
    private final Double individualReviewScore;
    private final Double individualPrScore;

    private final Double individualScore;
    private final Double relativeScore;
    private final Double deliveryScore;

    public DeliveryMetricsDto(
            String employeeId,
            String jobRole,
            Double issueResolutionTime,
            Double mergeTime,
            Double reviewRate,
            Integer prVolume,
            Integer reviewedPrCount,
            Double peerMean,
            Double adjustedReviewRate,
            Double relativeIssueScore,
            Double relativeMergeScore,
            Double relativeReviewScore,
            Double relativePrScore,
            Double individualIssueScore,
            Double individualMergeScore,
            Double individualReviewScore,
            Double individualPrScore,
            Double individualScore,
            Double relativeScore,
            Double deliveryScore
    ) {
        this(
                employeeId, jobRole,
                issueResolutionTime, mergeTime, reviewRate,
                prVolume, reviewedPrCount,
                peerMean, adjustedReviewRate,
                relativeIssueScore, relativeMergeScore,
                relativeReviewScore, relativePrScore,
                null, null, null, null,
                individualIssueScore, individualMergeScore,
                individualReviewScore, individualPrScore,
                individualScore, relativeScore, deliveryScore
        );
    }

    public DeliveryMetricsDto(
            String employeeId,
            String jobRole,
            Double issueResolutionTime,
            Double mergeTime,
            Double reviewRate,
            Integer prVolume,
            Integer reviewedPrCount,
            Double peerMean,
            Double adjustedReviewRate,
            Double relativeIssueScore,
            Double relativeMergeScore,
            Double relativeReviewScore,
            Double relativePrScore,
            Integer totalFuncionariosIssue,
            Integer totalFuncionariosMerge,
            Integer totalFuncionariosReview,
            Integer totalFuncionariosPr,
            Double individualIssueScore,
            Double individualMergeScore,
            Double individualReviewScore,
            Double individualPrScore,
            Double individualScore,
            Double relativeScore,
            Double deliveryScore
    ) {
        this.employeeId = employeeId;
        this.jobRole = jobRole;
        this.issueResolutionTime = issueResolutionTime;
        this.mergeTime = mergeTime;
        this.reviewRate = reviewRate;
        this.prVolume = prVolume;
        this.reviewedPrCount = reviewedPrCount;
        this.peerMean = peerMean;
        this.adjustedReviewRate = adjustedReviewRate;

        this.relativeIssueScore = relativeIssueScore;
        this.relativeMergeScore = relativeMergeScore;
        this.relativeReviewScore = relativeReviewScore;
        this.relativePrScore = relativePrScore;

        this.totalFuncionariosIssue = totalFuncionariosIssue;
        this.totalFuncionariosMerge = totalFuncionariosMerge;
        this.totalFuncionariosReview = totalFuncionariosReview;
        this.totalFuncionariosPr = totalFuncionariosPr;

        this.individualIssueScore = individualIssueScore;
        this.individualMergeScore = individualMergeScore;
        this.individualReviewScore = individualReviewScore;
        this.individualPrScore = individualPrScore;

        this.individualScore = individualScore;
        this.relativeScore = relativeScore;
        this.deliveryScore = deliveryScore;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getJobRole() {
        return jobRole;
    }

    public Double getIssueResolutionTime() {
        return issueResolutionTime;
    }

    public Double getMergeTime() {
        return mergeTime;
    }

    public Double getReviewRate() {
        return reviewRate;
    }

    public Integer getPrVolume() {
        return prVolume;
    }

    public Integer getReviewedPrCount() {
        return reviewedPrCount;
    }

    public Double getPeerMean() {
        return peerMean;
    }

    public Double getAdjustedReviewRate() {
        return adjustedReviewRate;
    }

    public Double getRelativeIssueScore() {
        return relativeIssueScore;
    }

    public Double getRelativeMergeScore() {
        return relativeMergeScore;
    }

    public Double getRelativeReviewScore() {
        return relativeReviewScore;
    }

    public Double getRelativePrScore() {
        return relativePrScore;
    }

    public Integer getTotalFuncionariosIssue() {
        return totalFuncionariosIssue;
    }

    public Integer getTotalFuncionariosMerge() {
        return totalFuncionariosMerge;
    }

    public Integer getTotalFuncionariosReview() {
        return totalFuncionariosReview;
    }

    public Integer getTotalFuncionariosPr() {
        return totalFuncionariosPr;
    }

    public Double getIndividualIssueScore() {
        return individualIssueScore;
    }

    public Double getIndividualMergeScore() {
        return individualMergeScore;
    }

    public Double getIndividualReviewScore() {
        return individualReviewScore;
    }

    public Double getIndividualPrScore() {
        return individualPrScore;
    }

    public Double getIndividualScore() {
        return individualScore;
    }

    public Double getRelativeScore() {
        return relativeScore;
    }

    public Double getDeliveryScore() {
        return deliveryScore;
    }
}