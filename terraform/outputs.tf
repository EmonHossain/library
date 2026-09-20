output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.library.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.library.name
}

output "ecs_task_definition_arn" {
  description = "ECS task definition ARN"
  value       = aws_ecs_task_definition.library.arn
}