-- Executed automatically, ONCE, by the official postgres image the
-- first time its data volume is empty (i.e. first container startup) —
-- anything placed in /docker-entrypoint-initdb.d/ inside that image
-- gets run this way, a convention of the official image itself, not
-- something we're configuring manually.
CREATE DATABASE identity_db;
CREATE DATABASE catalog_db;
CREATE DATABASE cart_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE review_db;