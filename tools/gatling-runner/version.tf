terraform {
  required_version = ">= 0.12"
  backend "s3" {
    bucket = "j5ik2o-terraform-state"
    # 作成したS3バケット
    region = "ap-northeast-1"
    key = "gatling.tfstate"
    profile = "cw-test"
    encrypt = true
  }
}