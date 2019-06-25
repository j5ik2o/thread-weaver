provider "kubernetes" {
}

resource "kubernetes_secret" "aurora" {
  metadata {
    name = "thread-weaver-app-secrets"
    namespace = "thread-weaver"
  }

  data {
    mysql.password = "${var.aurora_db_master_password}"
  }

  type = "Opaque"

}