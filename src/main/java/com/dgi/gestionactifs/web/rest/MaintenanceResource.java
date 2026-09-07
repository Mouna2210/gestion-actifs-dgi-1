package com.dgi.gestionactifs.web.rest;

import com.dgi.gestionactifs.repository.AffectationRepository;
import com.dgi.gestionactifs.repository.MaintenanceRepository;
import com.dgi.gestionactifs.security.SecurityUtils;
import com.dgi.gestionactifs.service.MaintenanceQueryService;
import com.dgi.gestionactifs.service.MaintenanceService;
import com.dgi.gestionactifs.service.criteria.MaintenanceCriteria;
import com.dgi.gestionactifs.service.dto.MaintenanceDTO;
import com.dgi.gestionactifs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.service.filter.LongFilter;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.dgi.gestionactifs.domain.Maintenance}.
 */
@RestController
@RequestMapping("/api/maintenances")
public class MaintenanceResource {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceResource.class);

    private static final String ENTITY_NAME = "maintenance";

    @Value("${jhipster.clientApp.name:gestionActifsDgi}")
    private String applicationName;

    private final MaintenanceService maintenanceService;

    private final MaintenanceRepository maintenanceRepository;

    private final MaintenanceQueryService maintenanceQueryService;

    private final AffectationRepository affectationRepository;

    public MaintenanceResource(
        MaintenanceService maintenanceService,
        MaintenanceRepository maintenanceRepository,
        MaintenanceQueryService maintenanceQueryService,
        AffectationRepository affectationRepository
    ) {
        this.maintenanceService = maintenanceService;
        this.maintenanceRepository = maintenanceRepository;
        this.maintenanceQueryService = maintenanceQueryService;
        this.affectationRepository = affectationRepository;
    }

    /**
     * {@code POST  /maintenances} : Create a new maintenance.
     *
     * @param maintenanceDTO the maintenanceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new maintenanceDTO, or with status {@code 400 (Bad Request)} if the maintenance has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_AGENT')")
    @PostMapping("")
    public ResponseEntity<MaintenanceDTO> createMaintenance(@Valid @RequestBody MaintenanceDTO maintenanceDTO) throws URISyntaxException {
        LOG.debug("REST request to save Maintenance : {}", maintenanceDTO);
        if (maintenanceDTO.getId() != null) {
            throw new BadRequestAlertException("A new maintenance cannot already have an ID", ENTITY_NAME, "idexists");
        }
        maintenanceDTO = maintenanceService.save(maintenanceDTO);
        return ResponseEntity.created(new URI("/api/maintenances/" + maintenanceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, maintenanceDTO.getId().toString()))
            .body(maintenanceDTO);
    }

    /**
     * {@code PUT  /maintenances/:id} : Updates an existing maintenance.
     *
     * @param id the id of the maintenanceDTO to save.
     * @param maintenanceDTO the maintenanceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated maintenanceDTO,
     * or with status {@code 400 (Bad Request)} if the maintenanceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the maintenanceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE')")
    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> updateMaintenance(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody MaintenanceDTO maintenanceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Maintenance : {}, {}", id, maintenanceDTO);
        if (maintenanceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, maintenanceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!maintenanceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        maintenanceDTO = maintenanceService.update(maintenanceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, maintenanceDTO.getId().toString()))
            .body(maintenanceDTO);
    }

    /**
     * {@code PATCH  /maintenances/:id} : Partial updates given fields of an existing maintenance, field will ignore if it is null
     *
     * @param id the id of the maintenanceDTO to save.
     * @param maintenanceDTO the maintenanceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated maintenanceDTO,
     * or with status {@code 400 (Bad Request)} if the maintenanceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the maintenanceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the maintenanceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MaintenanceDTO> partialUpdateMaintenance(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody MaintenanceDTO maintenanceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Maintenance partially : {}, {}", id, maintenanceDTO);
        if (maintenanceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, maintenanceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!maintenanceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MaintenanceDTO> result = maintenanceService.partialUpdate(maintenanceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, maintenanceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /maintenances} : get all the Maintenances.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Maintenances in body.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("")
    public ResponseEntity<List<MaintenanceDTO>> getAllMaintenances(
        MaintenanceCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Maintenances by criteria: {}", criteria);

        boolean isAgentOnly =
            SecurityUtils.hasCurrentUserThisAuthority("ROLE_AGENT") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_TECHNICIEN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_RESPONSABLE");

        if (isAgentOnly) {
            List<Long> actifIds = affectationRepository
                .findByUtilisateur_LoginAndDateRestitutionIsNull(SecurityUtils.getCurrentUserLogin().orElse(""))
                .stream()
                .map(affectation -> affectation.getActif().getId())
                .distinct()
                .toList();
            if (actifIds.isEmpty()) {
                HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                    ServletUriComponentsBuilder.fromCurrentRequest(),
                    Page.empty(pageable)
                );
                return ResponseEntity.ok().headers(headers).body(List.of());
            }
            LongFilter actifIdFilter = new LongFilter();
            actifIdFilter.setIn(actifIds);
            criteria.setActifId(actifIdFilter);
        }

        Page<MaintenanceDTO> page = maintenanceQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /maintenances/count} : count all the maintenances.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMaintenances(MaintenanceCriteria criteria) {
        LOG.debug("REST request to count Maintenances by criteria: {}", criteria);
        boolean isAgentOnlyCount =
            SecurityUtils.hasCurrentUserThisAuthority("ROLE_AGENT") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_TECHNICIEN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_RESPONSABLE");
        if (isAgentOnlyCount) {
            List<Long> actifIds = affectationRepository
                .findByUtilisateur_LoginAndDateRestitutionIsNull(SecurityUtils.getCurrentUserLogin().orElse(""))
                .stream()
                .map(affectation -> affectation.getActif().getId())
                .distinct()
                .toList();
            if (actifIds.isEmpty()) {
                return ResponseEntity.ok().body(0L);
            }
            LongFilter actifIdFilter = new LongFilter();
            actifIdFilter.setIn(actifIds);
            criteria.setActifId(actifIdFilter);
        }
        return ResponseEntity.ok().body(maintenanceQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /maintenances/:id} : get the "id" maintenance.
     *
     * @param id the id of the maintenanceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the maintenanceDTO, or with status {@code 404 (Not Found)}.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> getMaintenance(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Maintenance : {}", id);
        Optional<MaintenanceDTO> maintenanceDTO = maintenanceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(maintenanceDTO);
    }

    /**
     * {@code DELETE  /maintenances/:id} : delete the "id" maintenance.
     *
     * @param id the id of the maintenanceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Maintenance : {}", id);
        maintenanceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
