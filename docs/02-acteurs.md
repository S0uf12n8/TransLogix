# Acteurs du système — TransLogix

> Document de travail (brouillon). Un acteur est un **rôle** tenu par un utilisateur
> ou un système externe, pas une personne précise : une même personne peut cumuler
> plusieurs rôles. La numérotation des cas d'utilisation (UC) est provisoire.

## 1. Acteurs principaux

| Acteur | Description | Objectifs principaux | Cas d'utilisation clés |
|---|---|---|---|
| **Administrateur** | Gérant de l'entreprise de transport, responsable des données de référence et de la facturation | Tenir à jour clients, véhicules, chauffeurs et comptes ; facturer chaque mission livrée ; suivre l'activité | UC02, UC03, UC04, UC05, UC12, UC13, UC14 |
| **Dispatcheur** | Personne qui planifie les transports depuis le bureau | Créer les missions ; affecter un véhicule et un chauffeur disponibles sans conflit ; suivre l'avancement | UC06, UC07, UC08, UC11, UC14 |
| **Chauffeur** | Conducteur qui exécute les missions sur la route | Connaître les missions qui lui sont confiées ; signaler leur avancement | UC09, UC10 |

Tous les acteurs principaux s'authentifient (UC01).

## 2. Principes d'accès (moindre privilège)

- Chaque acteur n'accède qu'aux écrans et aux données utiles à ses objectifs.
- Un chauffeur ne voit que les missions qui lui sont affectées.
- La facturation est réservée à l'administrateur.
- Les droits sont vérifiés côté serveur, et pas seulement en masquant des boutons.

## 3. Relations entre acteurs

L'administrateur peut réaliser tout ce que fait le dispatcheur (généralisation
Administrateur → Dispatcheur). Cela reflète les petites entreprises où le gérant
planifie lui-même les missions.

## 4. Hors périmètre du MVP

- **Client** : destinataire de la facture, sans accès au système. Un portail client est une évolution possible.
- **Systèmes externes** (GPS, messagerie, paiement en ligne) : exclus.

## 5. Hypothèses à valider

- H1 : dans une petite entreprise, une même personne peut cumuler les rôles d'administrateur et de dispatcheur (hypothèse à valider auprès d'un professionnel du secteur). Trois rôles sont conservés pour séparer les responsabilités.
- H2 : le chauffeur utilise un navigateur sur téléphone ; pas d'application mobile native.
- H3 : le chauffeur ne crée ni ne modifie de mission ; il met à jour uniquement le statut des siennes.
- H4 : l'administrateur crée les comptes et attribue les rôles ; un utilisateur a un seul rôle.
