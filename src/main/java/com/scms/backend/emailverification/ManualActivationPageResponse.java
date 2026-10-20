package com.scms.backend.emailverification;
import java.util.List;
public record ManualActivationPageResponse(List<ManualActivationResponse> content,int page,int size,long totalElements,int totalPages) { }
