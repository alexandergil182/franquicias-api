terraform {
  required_version = ">= 1.5"

  required_providers {
    mongodbatlas = {
      source  = "mongodb/mongodbatlas"
      version = "~> 2.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

# Las llaves de API de Atlas se leen de las variables de entorno
# MONGODB_ATLAS_PUBLIC_KEY y MONGODB_ATLAS_PRIVATE_KEY (nunca se escriben en el código).
provider "mongodbatlas" {}
