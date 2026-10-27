variable "atlas_org_id" {
  description = "ID de la organización de MongoDB Atlas donde se crea el proyecto"
  type        = string
}

variable "project_name" {
  description = "Nombre del proyecto en Atlas"
  type        = string
  default     = "franquicias-api"
}

variable "cluster_name" {
  description = "Nombre del cluster"
  type        = string
  default     = "franquicias"
}

variable "aws_region" {
  description = "Región de AWS (formato Atlas) donde vive el cluster"
  type        = string
  default     = "US_EAST_1"
}

variable "db_user" {
  description = "Usuario de base de datos que usará la aplicación"
  type        = string
  default     = "franquicias_app"
}

variable "allowed_cidr" {
  description = "Rango IP que puede conectarse a Atlas (por ejemplo la IP de salida del NAT/ECS). Sin valor por defecto a propósito: no abras 0.0.0.0/0."
  type        = string
}
