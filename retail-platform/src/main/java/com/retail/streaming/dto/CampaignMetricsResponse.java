package com.retail.streaming.dto;

public record CampaignMetricsResponse (
	
	String campaignId,
	long impressions,
	long clicks,
	long basketadditions,
	long productviews
	)
{}
