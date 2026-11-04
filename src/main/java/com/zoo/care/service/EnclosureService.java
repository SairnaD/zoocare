package com.zoo.care.service;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.EnclosureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnclosureService {

    private final EnclosureRepository enclosureRepository;

    public List<EnclosureResponse> findAll() {
        return enclosureRepository.findAll(Sort.by("name")).stream()
                .map(EnclosureResponse::from)
                .toList();
    }

    public EnclosureResponse findById(Long id) {
        return EnclosureResponse.from(getEnclosure(id));
    }

    @Transactional
    public EnclosureResponse create(EnclosureRequest request) {
        String name = request.name().trim();
        if (enclosureRepository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Enclosure enclosure = Enclosure.builder()
                .name(name)
                .habitatType(request.habitatType())
                .capacity(request.capacity())
                .build();
        Enclosure saved = enclosureRepository.save(enclosure);
        log.info("Created enclosure '{}' (id={})", saved.getName(), saved.getId());
        return EnclosureResponse.from(saved);
    }

    @Transactional
    public EnclosureResponse update(Long id, EnclosureRequest request) {
        Enclosure enclosure = getEnclosure(id);
        String name = request.name().trim();
        if (enclosureRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        enclosure.setName(name);
        enclosure.setHabitatType(request.habitatType());
        enclosure.setCapacity(request.capacity());
        log.info("Updated enclosure '{}' (id={})", name, id);
        return EnclosureResponse.from(enclosure);
    }

    @Transactional
    public void delete(Long id) {
        Enclosure enclosure = getEnclosure(id);
        enclosureRepository.delete(enclosure);
        log.info("Deleted enclosure '{}' (id={})", enclosure.getName(), id);
    }

    private Enclosure getEnclosure(Long id) {
        return enclosureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", id));
    }

    private BusinessRuleException duplicateName(String name) {
        return new BusinessRuleException("Enclosure with name '" + name + "' already exists");
    }
}