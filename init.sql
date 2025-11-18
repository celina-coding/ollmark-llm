CREATE TABLE IF NOT EXISTS boutique (
    id_boutique SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    slogan TEXT,
    type VARCHAR(50) NOT NULL,
    adresse VARCHAR(255) NOT NULL,
    code_postal VARCHAR(10) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    pays VARCHAR(50) DEFAULT 'France',
    telephone VARCHAR(20),
    email VARCHAR(100),
    site_web VARCHAR(255),
    horaires TEXT,
    retrait BOOLEAN DEFAULT FALSE,
    livraison BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS produit (
    id_produit SERIAL PRIMARY KEY,
    id_boutique INTEGER NOT NULL REFERENCES boutique(id_boutique),
    nom VARCHAR(100) NOT NULL,
    origine VARCHAR(100),
    quantite VARCHAR(100),
    prix DECIMAL(10,2) NOT NULL
);
INSERT INTO boutique (nom, slogan, type, adresse, code_postal, ville, telephone, email, horaires, retrait, livraison) 
VALUES 
(
    'Boucherie Huet - Versailles', 
    'Une histoire familiale depuis les années 60', 
    'Boucherie', 
    '36 Rue du Maréchal Foch', 
    '78000', 
    'Versailles', 
    '+33 1 39 50 09 89', 
    'contact@boucheriehuet.fr', 
    'Lun-Ven: 7h-13h, 16h-20h | Sam: 7h-20h | Dim: 7h-13h', 
    true, 
    true
),
(
    'La Fromagerie du Marché', 
    'Fromages d''exception depuis 1985', 
    'Fromagerie', 
    '25 Rue du Commerce', 
    '75015', 
    'Paris', 
    '+33 1 42 50 68 90', 
    'info@fromagerie-marche.fr', 
    'Mar-Sam: 9h-13h, 16h-19h | Dim: 9h-13h', 
    true, 
    false
);

INSERT INTO produit (id_boutique, nom, origine, quantite, prix) 
VALUES 
(1, 'Côte de Boeuf Charolaise', 'France', '1.2 kg', 55.90),
(1, 'Filet Mignon', 'France', '800 g', 39.50),
(1, 'Saucisses Maison', 'France', '1 kg', 12.80),
(2, 'Comté 18 mois', 'France', '1 kg', 24.50),
(2, 'Camembert de Normandie AOP', 'France', '1 pièce', 4.20);