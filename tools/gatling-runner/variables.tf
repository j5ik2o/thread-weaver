variable "S3PrivateRegistryBucketName" {
  default = ""
}

variable "MaxSize" {
  default = 1
}

variable "DesiredCapacity" {
  default = 1
}

variable "S3GatlingLogBucketName" {
  default = "thread-weaver-gatling"
}

variable "GatlingWriteBaseURL" {
  default = ""
}

variable "GatlingReadBaseURL" {
  default = ""
}