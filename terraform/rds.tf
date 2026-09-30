resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-rds-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id] # only EKS nodes can reach MySQL, never public
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# One RDS instance, seven databases (database-per-service pattern kept — just on shared hardware
# to start; split into per-service RDS instances once any single service's load justifies it).
resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-mysql"
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100 # auto-scales storage, never compute — resize instance_class manually
  storage_encrypted     = true

  db_name  = "labourse_auth" # first DB created automatically; mysql-init/*.sql creates the rest
  username = "labourse_admin"
  password = var.db_password # from terraform.tfvars, NEVER committed — see README

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = 7
  multi_az                = false # flip to true once you have paying customers — doubles cost
  skip_final_snapshot     = false
  final_snapshot_identifier = "${var.project_name}-mysql-final"

  tags = { Project = var.project_name }
}
