package ma.translogix.dao;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import ma.translogix.model.Paie;

/** Payroll records are append-only: a validated or paid record is never deleted. */
public interface PaieDao {
    Paie save(Paie paie);

    void update(Paie paie);

    List<Paie> findAll();

    Optional<Paie> findById(Long id);

    Optional<Paie> findByEmployeEtPeriode(Long employeId, YearMonth periode);

    Optional<Paie> findByNumero(String numero);
}
