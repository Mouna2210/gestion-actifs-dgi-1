package com.dgi.gestionactifs.service.impl;

import com.dgi.gestionactifs.domain.Transfert;
import com.dgi.gestionactifs.domain.enumeration.StatutTransfert;
import com.dgi.gestionactifs.repository.TransfertRepository;
import com.dgi.gestionactifs.service.TransfertService;
import com.dgi.gestionactifs.service.dto.TransfertDTO;
import com.dgi.gestionactifs.service.mapper.TransfertMapper;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service Implementation for managing {@link com.dgi.gestionactifs.domain.Transfert}.
 */
@Service
@Transactional
public class TransfertServiceImpl implements TransfertService {

    private static final Logger LOG = LoggerFactory.getLogger(TransfertServiceImpl.class);

    private final TransfertRepository transfertRepository;

    private final TransfertMapper transfertMapper;

    public TransfertServiceImpl(TransfertRepository transfertRepository, TransfertMapper transfertMapper) {
        this.transfertRepository = transfertRepository;
        this.transfertMapper = transfertMapper;
    }

    @Override
    public TransfertDTO save(TransfertDTO transfertDTO) {
        LOG.debug("Request to save Transfert : {}", transfertDTO);
        Transfert transfert = transfertMapper.toEntity(transfertDTO);
        transfert = transfertRepository.save(transfert);
        return transfertMapper.toDto(transfert);
    }

    @Override
    public TransfertDTO update(TransfertDTO transfertDTO) {
        LOG.debug("Request to update Transfert : {}", transfertDTO);
        Transfert transfert = transfertMapper.toEntity(transfertDTO);
        transfert = transfertRepository.save(transfert);
        return transfertMapper.toDto(transfert);
    }

    @Override
    public Optional<TransfertDTO> partialUpdate(TransfertDTO transfertDTO) {
        LOG.debug("Request to partially update Transfert : {}", transfertDTO);

        return transfertRepository
            .findById(transfertDTO.getId())
            .map(existingTransfert -> {
                transfertMapper.partialUpdate(existingTransfert, transfertDTO);

                return existingTransfert;
            })
            .map(transfertRepository::save)
            .map(transfertMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransfertDTO> findOne(Long id) {
        LOG.debug("Request to get Transfert : {}", id);
        return transfertRepository.findById(id).map(transfertMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Transfert : {}", id);
        transfertRepository.deleteById(id);
    }

    @Override
    public TransfertDTO valider(Long id) {
        LOG.debug("Request to valider Transfert : {}", id);
        Transfert transfert = transfertRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfert introuvable"));
        if (transfert.getStatut() != StatutTransfert.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un transfert en attente peut etre valide");
        }
        transfert.setStatut(StatutTransfert.VALIDE);
        transfert.setDateTraitement(LocalDate.now());
        return transfertMapper.toDto(transfertRepository.save(transfert));
    }

    @Override
    public TransfertDTO rejeter(Long id, String commentaireRejet) {
        LOG.debug("Request to rejeter Transfert : {}", id);
        if (commentaireRejet == null || commentaireRejet.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un commentaire de rejet est obligatoire");
        }
        Transfert transfert = transfertRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transfert introuvable"));
        if (transfert.getStatut() != StatutTransfert.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un transfert en attente peut etre rejete");
        }
        transfert.setStatut(StatutTransfert.REJETE);
        transfert.setCommentaireRejet(commentaireRejet);
        transfert.setDateTraitement(LocalDate.now());
        return transfertMapper.toDto(transfertRepository.save(transfert));
    }
}
