package com.dgi.gestionactifs.web.rest;

import com.dgi.gestionactifs.repository.AffectationRepository;
import com.dgi.gestionactifs.repository.TransfertRepository;
import com.dgi.gestionactifs.security.SecurityUtils;
import com.dgi.gestionactifs.service.TransfertQueryService;
import com.dgi.gestionactifs.service.TransfertService;
import com.dgi.gestionactifs.service.criteria.TransfertCriteria;
import com.dgi.gestionactifs.service.dto.TransfertDTO;
import com.dgi.gestionactifs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
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
 * REST controller for managing {@link com.dgi.gestionactifs.domain.Transfert}.
 */
@RestController
@RequestMapping("/api/transferts")
public class TransfertResource {

    private static final Logger LOG = LoggerFactory.getLogger(TransfertResource.class);

    private static final String ENTITY_NAME = "transfert";

    @Value("${jhipster.clientApp.name:gestionActifsDgi}")
    private String applicationName;

    private final TransfertService transfertService;

    private final TransfertRepository transfertRepository;

    private final TransfertQueryService transfertQueryService;

    private final AffectationRepository affectationRepository;

    public TransfertResource(
        TransfertService transfertService,
        TransfertRepository transfertRepository,
        TransfertQueryService transfertQueryService,
        AffectationRepository affectationRepository
    ) {
        this.transfertService = transfertService;
        this.transfertRepository = transfertRepository;
        this.transfertQueryService = transfertQueryService;
        this.affectationRepository = affectationRepository;
    }

    /**
     * {@code POST  /transferts} : Create a new transfert.
     *
     * @param transfertDTO the transfertDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new transfertDTO, or with status {@code 400 (Bad Request)} if the transfert has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_AGENT')")
    @PostMapping("")
    public ResponseEntity<TransfertDTO> createTransfert(@Valid @RequestBody TransfertDTO transfertDTO) throws URISyntaxException {
        LOG.debug("REST request to save Transfert : {}", transfertDTO);
        if (transfertDTO.getId() != null) {
            throw new BadRequestAlertException("A new transfert cannot already have an ID", ENTITY_NAME, "idexists");
        }
        transfertDTO = transfertService.save(transfertDTO);
        return ResponseEntity.created(new URI("/api/transferts/" + transfertDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, transfertDTO.getId().toString()))
            .body(transfertDTO);
    }

    /**
     * {@code PUT  /transferts/:id} : Updates an existing transfert.
     *
     * @param id the id of the transfertDTO to save.
     * @param transfertDTO the transfertDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated transfertDTO,
     * or with status {@code 400 (Bad Request)} if the transfertDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the transfertDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE')")
    @PutMapping("/{id}")
    public ResponseEntity<TransfertDTO> updateTransfert(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody TransfertDTO transfertDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Transfert : {}, {}", id, transfertDTO);
        if (transfertDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, transfertDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!transfertRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        transfertDTO = transfertService.update(transfertDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, transfertDTO.getId().toString()))
            .body(transfertDTO);
    }

    /**
     * {@code PATCH  /transferts/:id} : Partial updates given fields of an existing transfert, field will ignore if it is null
     *
     * @param id the id of the transfertDTO to save.
     * @param transfertDTO the transfertDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated transfertDTO,
     * or with status {@code 400 (Bad Request)} if the transfertDTO is not valid,
     * or with status {@code 404 (Not Found)} if the transfertDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the transfertDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TransfertDTO> partialUpdateTransfert(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody TransfertDTO transfertDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Transfert partially : {}, {}", id, transfertDTO);
        if (transfertDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, transfertDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!transfertRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<TransfertDTO> result = transfertService.partialUpdate(transfertDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, transfertDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /transferts} : get all the Transferts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Transferts in body.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("")
    public ResponseEntity<List<TransfertDTO>> getAllTransferts(
        TransfertCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Transferts by criteria: {}", criteria);

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

        Page<TransfertDTO> page = transfertQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /transferts/count} : count all the transferts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countTransferts(TransfertCriteria criteria) {
        LOG.debug("REST request to count Transferts by criteria: {}", criteria);
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
        return ResponseEntity.ok().body(transfertQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /transferts/:id} : get the "id" transfert.
     *
     * @param id the id of the transfertDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the transfertDTO, or with status {@code 404 (Not Found)}.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<TransfertDTO> getTransfert(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Transfert : {}", id);
        Optional<TransfertDTO> transfertDTO = transfertService.findOne(id);
        return ResponseUtil.wrapOrNotFound(transfertDTO);
    }

    /**
     * {@code DELETE  /transferts/:id} : delete the "id" transfert.
     *
     * @param id the id of the transfertDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfert(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Transfert : {}", id);
        transfertService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code PATCH  /transferts/:id/valider} : valide un transfert en attente.
     *
     * @param id l'id du transfert a valider.
     * @return le transfert mis a jour.
     */
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE')")
    @PatchMapping("/{id}/valider")
    public ResponseEntity<TransfertDTO> validerTransfert(@PathVariable Long id) {
        LOG.debug("REST request to valider Transfert : {}", id);
        TransfertDTO result = transfertService.valider(id);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code PATCH  /transferts/:id/rejeter} : rejette un transfert en attente.
     *
     * @param id l'id du transfert a rejeter.
     * @param body doit contenir la cle "commentaireRejet" (obligatoire).
     * @return le transfert mis a jour.
     */
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE')")
    @PatchMapping("/{id}/rejeter")
    public ResponseEntity<TransfertDTO> rejeterTransfert(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LOG.debug("REST request to rejeter Transfert : {}", id);
        TransfertDTO result = transfertService.rejeter(id, body.get("commentaireRejet"));
        return ResponseEntity.ok(result);
    }
}
