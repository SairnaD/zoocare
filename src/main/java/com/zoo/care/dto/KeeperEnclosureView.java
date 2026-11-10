package com.zoo.care.dto;

import java.util.List;

public record KeeperEnclosureView(EnclosureResponse enclosure, List<AnimalResponse> animals) {
}
