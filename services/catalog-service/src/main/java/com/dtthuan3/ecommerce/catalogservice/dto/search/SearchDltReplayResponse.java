package com.dtthuan3.ecommerce.catalogservice.dto.search;

public record SearchDltReplayResponse(String topic, int partition, long offset, String result) { }
