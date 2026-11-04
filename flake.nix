{
  description = "buni: the hidden network shield development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
          };
        };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            go
            gopls
            jdk17
            android-tools
            gnumake
            git
          ];

          shellHook = ''
            export JAVA_HOME=${pkgs.jdk17}
            export PATH=$JAVA_HOME/bin:$PATH
          '';
        };
      }
    );
}
