resource "aws_ecr_repository" "gatling-runner-ecr" {
  name = "${var.gatling-runner-ecr-name}"
}

resource "aws_ecr_repository_policy" "gatling-runner-ecr-policy" {
  policy = <<EOF
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Sid": "gaudi-poc-gatling-ecs-runner-ecr",
            "Effect": "Allow",
            "Principal": "*",
            "Action": [
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:BatchCheckLayerAvailability",
                "ecr:PutImage",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload",
                "ecr:DescribeRepositories",
                "ecr:GetRepositoryPolicy",
                "ecr:ListImages",
                "ecr:DeleteRepository",
                "ecr:BatchDeleteImage",
                "ecr:SetRepositoryPolicy",
                "ecr:DeleteRepositoryPolicy"
            ]
        }
    ]
}
EOF


  repository = aws_ecr_repository.gatling-runner-ecr.name
}

resource "aws_ecr_repository" "gatling-s3-reporter-ecr" {
  name = "${var.gatling-s3-reporter-ecr-name}"
}

resource "aws_ecr_repository_policy" "gatling-s3-reporter-ecr-policy" {
  policy = <<EOF
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Sid": "gaudi-poc-gatling-ecs-s3-reporter-ecr",
            "Effect": "Allow",
            "Principal": "*",
            "Action": [
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:BatchCheckLayerAvailability",
                "ecr:PutImage",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload",
                "ecr:DescribeRepositories",
                "ecr:GetRepositoryPolicy",
                "ecr:ListImages",
                "ecr:DeleteRepository",
                "ecr:BatchDeleteImage",
                "ecr:SetRepositoryPolicy",
                "ecr:DeleteRepositoryPolicy"
            ]
        }
    ]
}
EOF


  repository = aws_ecr_repository.gatling-s3-reporter-ecr.name
}