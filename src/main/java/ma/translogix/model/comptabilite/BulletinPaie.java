
package ma.translogix.model.comptabilite;

public class BulletinPaie {

    private Long id;
    private Long employeId;
    private String periode;
    private double salaireBrut;
    private double primes;
    private double retenues;
    private double netAPayer;

    public BulletinPaie() {
    }

    public BulletinPaie(Long id, Long employeId, String periode,
                        double salaireBrut, double primes,
                        double retenues, double netAPayer) {
        this.id = id;
        this.employeId = employeId;
        this.periode = periode;
        this.salaireBrut = salaireBrut;
        this.primes = primes;
        this.retenues = retenues;
        this.netAPayer = netAPayer;
    }

    public void calculerNet() {
        netAPayer = salaireBrut + primes - retenues;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeId() { return employeId; }
    public void setEmployeId(Long employeId) { this.employeId = employeId; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public double getSalaireBrut() { return salaireBrut; }
    public void setSalaireBrut(double salaireBrut) { this.salaireBrut = salaireBrut; }

    public double getPrimes() { return primes; }
    public void setPrimes(double primes) { this.primes = primes; }

    public double getRetenues() { return retenues; }
    public void setRetenues(double retenues) { this.retenues = retenues; }

    public double getNetAPayer() { return netAPayer; }
    public void setNetAPayer(double netAPayer) { this.netAPayer = netAPayer; }
}
