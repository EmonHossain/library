terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# ============================================================
# ECR REPOSITORY
# ============================================================

resource "aws_ecr_repository" "library" {
  name                 = "library-management"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# ============================================================
# ECS CLUSTER
# ============================================================

resource "aws_ecs_cluster" "library" {
  name = "library-management"
}

# ============================================================
# CLOUDWATCH LOG GROUP
# ============================================================

resource "aws_cloudwatch_log_group" "library" {
  name              = "/ecs/library-management"
  retention_in_days = 7
}

# ============================================================
# ECS TASK EXECUTION ROLE
# ============================================================

resource "aws_iam_role" "ecs_task_execution" {
  name = "library-management-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }

        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role = aws_iam_role.ecs_task_execution.name

  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ============================================================
# ECS TASK DEFINITION
# ============================================================

resource "aws_ecs_task_definition" "library" {
  family = "library-management"

  requires_compatibilities = [
    "FARGATE"
  ]

  network_mode = "awsvpc"

  cpu    = "256"
  memory = "512"

  execution_role_arn = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([
    {
      name = "library-management"

      image = "${aws_ecr_repository.library.repository_url}:latest"

      essential = true

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          awslogs-group         = aws_cloudwatch_log_group.library.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}