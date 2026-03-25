package com.example.user_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGruppoRequest {
    public String name;
    public int ownerId;
    public List<Integer> members;
}
