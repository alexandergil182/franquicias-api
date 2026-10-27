# Host del cluster (sin credenciales), p. ej. franquicias.abcde.mongodb.net
output "mongodb_host" {
  value = replace(mongodbatlas_advanced_cluster.franquicias.connection_strings.standard_srv, "mongodb+srv://", "")
}

# URI completa para la variable MONGODB_URI de la aplicación. Es sensible: sale solo con
#   terraform output -raw mongodb_uri
output "mongodb_uri" {
  value = format(
    "mongodb+srv://%s:%s@%s/franquicias?retryWrites=true&w=majority",
    mongodbatlas_database_user.app.username,
    random_password.db.result,
    replace(mongodbatlas_advanced_cluster.franquicias.connection_strings.standard_srv, "mongodb+srv://", "")
  )
  sensitive = true
}
