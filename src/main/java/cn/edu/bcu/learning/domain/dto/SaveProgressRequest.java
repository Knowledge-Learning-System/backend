package cn.edu.bcu.learning.domain.dto;

import lombok.Data;

@Data
public class SaveProgressRequest {

    private Integer videoId;

    private Double position;

    private Double progress;
}
