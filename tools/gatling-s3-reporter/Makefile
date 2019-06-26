DOCKER_PACKAGE := 738575627980.dkr.ecr.ap-northeast-1.amazonaws.com/chatwork/gaudi-poc-gatling-s3-reporter

.PHONY: build
build:
	docker build -t $(DOCKER_PACKAGE) .;
	@version=$$(docker inspect -f {{.Config.Labels.version}} $(DOCKER_PACKAGE)); \
		if [ -n "$$version" ]; then \
			docker tag $(DOCKER_PACKAGE):latest $(DOCKER_PACKAGE):$$version; \
		fi

.PHONY: push
push:
	@version=$$(docker inspect -f {{.Config.Labels.version}} $(DOCKER_PACKAGE)); \
		if docker inspect --format='{{index .RepoDigests 0}}' $(DOCKER_PACKAGE):$$version >/dev/null 2>&1; then \
			echo "no changes"; \
		else \
			docker push $(DOCKER_PACKAGE):latest; \
			docker tag $(DOCKER_PACKAGE) $(DOCKER_PACKAGE):$$version; \
			docker push $(DOCKER_PACKAGE):$$version; \
		fi