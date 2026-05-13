/**
 * Interface définissant le contrat pour la génération de rapports.
 * Toute classe capable de produire un rapport doit implémenter cette interface.
 */
public interface Reportable {
    /**
     * Génère un rapport textuel résumant les informations de l'entité.
     *
     * @return Une chaîne de caractères représentant le rapport.
     */
    String generateReport();
}
