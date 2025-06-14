# ✨ LlamaDupe

> **Plugin Minecraft 1.21.1** — Dupliquez vos items comme un pro !

> Ce plugin a été conçu pour le serveur anarchie 4B4T.

---

## ✨ Présentation

**LlamaDupe** est un plugin PaperMC/Spigot pour Minecraft 1.21.1 qui permet de dupliquer facilement l'inventaire des lamas, avec gestion avancée des shulker boxes, cooldowns, permissions et commandes d'administration. Idéal pour les serveurs fun, créatifs ou pour tester des mécaniques de duplication !

---

## 🚀 Fonctionnalités principales

- **Duplication d'inventaire de lama** :
  - Montez sur un lama, remplissez son inventaire, descendez, attendez le cooldown, puis remontez pour dupliquer !
- **Gestion avancée des shulker boxes** :
  - Les shulkers sont dupliquées proprement, avec leur contenu !
- **Cooldown anti-abus** :
  - 15 secondes entre chaque duplication par joueur.
- **Protection anti-tête de joueur** :
  - Les têtes de joueurs ne sont pas dupliquées (même dans les shulkers).
- **Commandes d'administration** :
  - `/llamadupe enable` ou `/llamadupe disable` pour activer/désactiver la fonctionnalité. (temporairement)
- **Logs détaillés** :
  - Toutes les actions importantes sont loguées côté serveur.

---

## 🛠️ Installation

1. **Téléchargez** le fichier `llamadupe-1.0.jar` depuis [GitHub](https://github.com/arockenbauer/llamadupe/releases)
2. **Placez** le fichier dans le dossier `plugins/` de votre serveur PaperMC/Spigot 1.21.1.
3. **Redémarrez** le serveur.
4. **(Optionnel)** : Vérifiez que le plugin est bien chargé avec `/plugins`.

---

## ⚡ Utilisation

### ➡️ Duplication
1. Montez sur un lama.
2. Placez les items à dupliquer dans l'inventaire du lama.
3. Descendez du lama (le cooldown démarre).
4. Attendez 15 secondes.
5. Remontez sur le même lama : les items sont dupliqués dans votre inventaire !

### ➡️ Commandes
- `/llamadupe enable` : Active la duplication.
- `/llamadupe disable` : Désactive la duplication.

> **Permission requise** : `llamadupe.admin`

---

## 🔒 Permissions

- `llamadupe.admin` :
  - Utilisation de la commande `/llamadupe`.

---

## 🧩 Dépendances

- **PaperMC API** 1.21.1-R0.1-SNAPSHOT
- **Java 17**

---

## 🏗️ Compilation

Ce projet utilise **Maven**.

```bash
mvn clean package
```

Le JAR sera généré dans `target/llamadupe-1.0.jar`.

---

## 📁 Structure du projet

```
llamadupe/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── fr/axel/llamadupe/LlamaDupePlugin.java
│       └── resources/
│           └── plugin.yml
└── target/
    └── llamadupe-1.0.jar
```

---

## 👨‍💻 Auteur

- **axel**

---

## 📝 Licence

Ce plugin est distribué sous licence GNU GPL 3.0. Voir le fichier `LICENSE`.

---

## 💡 Astuces & Remarques

- Les items sont donnés dans l'inventaire du joueur, ou droppés au sol si l'inventaire est plein.
- Les logs serveur permettent de suivre toutes les duplications et actions importantes.
- Les têtes de joueurs sont explicitement exclues de la duplication (même dans les shulkers).
- Le plugin est désactivable/activable à chaud via la commande `/llamadupe`.

---

## 🦙 Bonne duplication !
