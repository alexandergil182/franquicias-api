# Plantilla de ejemplo: aprovisiona la persistencia (MongoDB) en MongoDB Atlas sobre AWS.
# No está desplegada; ver el README para los pasos.

resource "mongodbatlas_project" "franquicias" {
  name   = var.project_name
  org_id = var.atlas_org_id
}

# Cluster gratuito M0 (suficiente para la prueba). Para producción usa M10+ y un replica set de 3 nodos.
resource "mongodbatlas_advanced_cluster" "franquicias" {
  project_id   = mongodbatlas_project.franquicias.id
  name         = var.cluster_name
  cluster_type = "REPLICASET"

  replication_specs = [{
    region_configs = [{
      provider_name         = "TENANT"
      backing_provider_name = "AWS"
      region_name           = var.aws_region
      priority              = 7
      electable_specs = {
        instance_size = "M0"
      }
    }]
  }]
}

# Contraseña generada por Terraform: no se escribe en el repositorio
resource "random_password" "db" {
  length  = 32
  special = false
}

resource "mongodbatlas_database_user" "app" {
  project_id         = mongodbatlas_project.franquicias.id
  auth_database_name = "admin"
  username           = var.db_user
  password           = random_password.db.result

  roles {
    role_name     = "readWrite"
    database_name = "franquicias"
  }
}

# Solo este rango IP puede abrir conexiones al cluster
resource "mongodbatlas_project_ip_access_list" "app" {
  project_id = mongodbatlas_project.franquicias.id
  cidr_block = var.allowed_cidr
  comment    = "Origen permitido para la API de franquicias"
}
