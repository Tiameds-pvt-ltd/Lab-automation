# Lab-automation
# test
## Environment variables

Copy `.env.example` to `.env` and fill in the values (`.env` is git-ignored). Spring Boot does not load `.env` by itself: set the same variables in your IDE Run Configuration (Environment variables) or in your shell before starting the app.

For lab logo / certificate uploads to S3 you need at least `AWS_S3_BUCKET`, `AWS_REGION` and AWS credentials (`AWS_ACCESS_KEY` + `AWS_SECRET_KEY`, or `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY`, or an IAM role in ECS). Never commit real keys.
