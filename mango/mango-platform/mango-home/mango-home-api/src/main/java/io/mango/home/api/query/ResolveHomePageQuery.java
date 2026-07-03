package io.mango.home.api.query;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResolveHomePageQuery implements Serializable {

    private Long homeId;
}
