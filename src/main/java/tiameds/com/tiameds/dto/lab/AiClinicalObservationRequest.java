package tiameds.com.tiameds.dto.lab;

public class AiClinicalObservationRequest {

    private String provisionalDiagnosis;
    private String clinicalInterpretation;
    private String doctorToVisit;
    private String patientInterpretation;
    private String tips;
    private String contentHash;

    public String getProvisionalDiagnosis() { return provisionalDiagnosis; }
    public void setProvisionalDiagnosis(String provisionalDiagnosis) { this.provisionalDiagnosis = provisionalDiagnosis; }

    public String getClinicalInterpretation() { return clinicalInterpretation; }
    public void setClinicalInterpretation(String clinicalInterpretation) { this.clinicalInterpretation = clinicalInterpretation; }

    public String getDoctorToVisit() { return doctorToVisit; }
    public void setDoctorToVisit(String doctorToVisit) { this.doctorToVisit = doctorToVisit; }

    public String getPatientInterpretation() { return patientInterpretation; }
    public void setPatientInterpretation(String patientInterpretation) { this.patientInterpretation = patientInterpretation; }

    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
